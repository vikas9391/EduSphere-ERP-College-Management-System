import axios, { AxiosError } from 'axios'
import { useAuthStore } from '@/store/authStore'
import type { ApiResponse } from './types'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api',
})

api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
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
    const body = response.data as ApiResponse<unknown> | unknown
    if (
      body &&
      typeof body === 'object' &&
      'success' in body &&
      typeof (body as { success: unknown }).success === 'boolean'
    ) {
      response.data = (body as ApiResponse<unknown>).data
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
  const backendMessage = error.response?.data?.message
  if (backendMessage) {
    error.message = backendMessage
  }
  return error
}
