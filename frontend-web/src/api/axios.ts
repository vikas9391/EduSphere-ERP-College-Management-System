import axios, { AxiosError, type AxiosResponse } from 'axios'
import { useAuthStore } from '@/store/authStore'
import type { ApiResponse } from './types'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api',
})

/**
 * Small browser-side response cache for the data-heavy ERP screens.
 *
 * Redis already protects the backend from repeated database work, but a browser
 * still has to wait for the network on every navigation. This cache makes repeat
 * GET requests instant for a short period and is automatically invalidated when
 * a POST/PUT/PATCH/DELETE succeeds. The key is scoped to the signed-in tenant
 * and user so cached data cannot cross account boundaries.
 */
const CLIENT_CACHE_PREFIX = 'edusphere:api-cache:v1:'
const CLIENT_CACHE_TTL_MS = 60_000

function clientCacheKey(config: { baseURL?: string; url?: string; params?: unknown }) {
  const user = useAuthStore.getState().user
  const scope = user ? `${user.tenantSchema}:${user.id}:${user.email}` : 'anonymous'
  const params = config.params ? JSON.stringify(config.params) : ''
  return CLIENT_CACHE_PREFIX + btoa(unescape(encodeURIComponent(
    `${scope}|${config.baseURL ?? ''}|${config.url ?? ''}|${params}`
  )))
}

function canCacheGet(config: { method?: string; url?: string }) {
  const method = (config.method ?? 'get').toLowerCase()
  const url = config.url ?? ''
  return method === 'get' && !url.startsWith('/auth/')
}

function isMutation(config: { method?: string }) {
  return ['post', 'put', 'patch', 'delete'].includes((config.method ?? '').toLowerCase())
}

function clearClientCache() {
  for (let i = localStorage.length - 1; i >= 0; i -= 1) {
    const key = localStorage.key(i)
    if (key?.startsWith(CLIENT_CACHE_PREFIX)) localStorage.removeItem(key)
  }
}

function readClientCache(key: string): unknown | null {
  try {
    const raw = localStorage.getItem(key)
    if (!raw) return null
    const entry = JSON.parse(raw) as { savedAt: number; data: unknown }
    if (!entry || Date.now() - entry.savedAt > CLIENT_CACHE_TTL_MS) {
      localStorage.removeItem(key)
      return null
    }
    return entry.data
  } catch {
    localStorage.removeItem(key)
    return null
  }
}

function writeClientCache(key: string, data: unknown) {
  try {
    localStorage.setItem(key, JSON.stringify({ savedAt: Date.now(), data }))
  } catch {
    // Storage quota/private-mode failures should never break the ERP.
  }
}

api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  // Serve recently loaded GET data directly from browser memory/storage.
  // Axios adapters let us short-circuit the network while keeping all existing
  // api.get(...) call sites unchanged.
  if (token && canCacheGet(config)) {
    const key = clientCacheKey(config)
    const cached = readClientCache(key)
    if (cached !== null) {
      config.adapter = async () => ({
        data: cached,
        status: 200,
        statusText: 'OK (client cache)',
        headers: {},
        config,
        request: undefined,
      } as AxiosResponse)
    } else {
      config.headers['X-EduSphere-Client-Cache'] = key
    }
  }

  // Any successful write will invalidate all browser GET snapshots. This keeps
  // list/detail screens from showing data changed by the current session.
  if (isMutation(config)) clearClientCache()

  return config
})

/**
 * Two jobs:
 *  1. Unwrap the backend's ApiResponse<T> envelope transparently, so every existing
 *     `res.data` call site across the app keeps working whether that endpoint has
 *     been migrated to the envelope yet or still returns a raw payload (see
 *     `unwrap`/`unwrapList` in ./types.ts for the paginated-list case, which needs
 *     one extra step at the call site since `content` must be pulled out explicitly).
 *  2. On error, extract the backend's `message` field (from GlobalExceptionHandler)
 *     into `error.message`, so `catch (err) { err.message }` call sites across the
 *     app show the real backend validation/business-rule message instead of a
 *     generic Axios "Request failed with status code 4xx" string.
 */
api.interceptors.response.use(
  (response) => {
    const originalConfig = response.config
    const body = response.data as ApiResponse<unknown> | unknown
    if (
      body &&
      typeof body === 'object' &&
      'success' in body &&
      typeof (body as { success: unknown }).success === 'boolean'
    ) {
      response.data = (body as ApiResponse<unknown>).data
    }

    if (canCacheGet(originalConfig) && useAuthStore.getState().token) {
      writeClientCache(clientCacheKey(originalConfig), response.data)
    }

    return response
  },
  (error: AxiosError<ApiResponse<unknown> | { message?: string }>) => onResponseError(error)
)

/**
 * A 401 almost always just means the 15-minute access token expired mid-session -
 * not that the user's credentials are actually invalid. Previously this logged the
 * user out immediately and bounced them to /login, which produced a "log in -> works
 * for a bit -> get bounced to /login again" loop as the token kept expiring. Now a
 * single 401 triggers one silent POST /auth/refresh using the stored refresh token,
 * and - if that succeeds - transparently retries the original request with the new
 * access token, so the user never notices their session renewing.
 *
 * `isRefreshing`/`pendingQueue` collapse concurrent 401s (e.g. a page firing several
 * requests at once) into a single /auth/refresh call: the first 401 starts the
 * refresh, every other 401 that arrives while it's in flight just waits on the same
 * promise instead of firing its own redundant refresh request.
 */
let isRefreshing = false
let pendingQueue: Array<(token: string | null) => void> = []

function onRefreshed(newToken: string | null) {
  pendingQueue.forEach((resolve) => resolve(newToken))
  pendingQueue = []
}

async function onResponseError(error: AxiosError<ApiResponse<unknown> | { message?: string }>) {
  const originalRequest = error.config as (typeof error.config & { _retried?: boolean }) | undefined
  const requestUrl = originalRequest?.url ?? ''

  const isAuthEndpoint = requestUrl.includes('/auth/login') || requestUrl.includes('/auth/refresh')

  if (error.response?.status === 401 && originalRequest && !originalRequest._retried && !isAuthEndpoint) {
    originalRequest._retried = true

    const { refreshToken } = useAuthStore.getState()

    if (!refreshToken) {
      useAuthStore.getState().logout()
      return Promise.reject(attachMessage(error))
    }

    if (isRefreshing) {
      // Another request already kicked off the refresh - wait for it instead of
      // firing a second one, then retry with whatever token it produced.
      const newToken = await new Promise<string | null>((resolve) => pendingQueue.push(resolve))
      if (!newToken) return Promise.reject(attachMessage(error))
      originalRequest.headers = originalRequest.headers ?? {}
      originalRequest.headers.Authorization = `Bearer ${newToken}`
      return api(originalRequest)
    }

    isRefreshing = true
    try {
      const res = await api.post<{
        accessToken: string
        refreshToken: string
      }>('/auth/refresh', { refreshToken })

      const { accessToken, refreshToken: newRefreshToken } = res.data
      useAuthStore.getState().setToken(accessToken)
      useAuthStore.getState().setRefreshToken(newRefreshToken)
      onRefreshed(accessToken)

      originalRequest.headers = originalRequest.headers ?? {}
      originalRequest.headers.Authorization = `Bearer ${accessToken}`
      return api(originalRequest)
    } catch {
      // The refresh token itself is invalid/expired - there's no way to silently
      // recover, so this is a real logout.
      onRefreshed(null)
      useAuthStore.getState().logout()
      return Promise.reject(attachMessage(error))
    } finally {
      isRefreshing = false
    }
  }

  return Promise.reject(attachMessage(error))
}

function attachMessage(error: AxiosError<ApiResponse<unknown> | { message?: string }>) {
  const responseData = error.response?.data
  const backendMessage = responseData?.message

  if (backendMessage) {
    // Validation errors are returned by GlobalExceptionHandler as:
    // { message: "Validation failed", data: { fieldName: "reason" } }.
    // Surface the field reason in the UI instead of leaving the user with only
    // the generic "Validation failed" message.
    const validationData =
      responseData &&
      typeof responseData === 'object' &&
      'data' in responseData &&
      responseData.data &&
      typeof responseData.data === 'object'
        ? responseData.data as Record<string, unknown>
        : null

    const fieldMessages = validationData
      ? Object.entries(validationData)
          .filter(([, value]) => typeof value === 'string' && value.trim())
          .map(([field, value]) => {
            const label = field.replace(/([A-Z])/g, ' $1').replace(/^./, (c) => c.toUpperCase())
            return `${label}: ${String(value)}`
          })
      : []

    error.message = fieldMessages.length
      ? `${backendMessage}: ${fieldMessages.join('; ')}`
      : backendMessage
  }

  return error
}
