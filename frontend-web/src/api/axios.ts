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
  (error: AxiosError<ApiResponse<unknown> | { message?: string }>) => {
    if (error.response?.status === 401) {
      useAuthStore.getState().logout()
    }

    const backendMessage = error.response?.data?.message
    if (backendMessage) {
      error.message = backendMessage
    }

    return Promise.reject(error)
  }
)
