/**
 * Mirrors com.collegeerp.Backend.common.dto.ApiResponse<T> exactly.
 * Every endpoint in an already-refactored backend package (auth, tenant, department,
 * course, subject, teacher, student, and all /api/student/* self-service endpoints)
 * wraps its payload in this envelope. Endpoints in packages not yet refactored on the
 * backend (attendance, assignment, marks, result, enrollment, examination admin CRUD)
 * still return their raw payload with no envelope - see `unwrap()` below, which handles
 * both transparently so callers never have to care which case they're in.
 */
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
  status?: number
  path?: string
  timestamp?: string
}

/**
 * Mirrors com.collegeerp.Backend.common.dto.PagedResponse<T> exactly.
 * Returned as the `data` of an ApiResponse for every paginated "list" endpoint
 * (departments, courses, subjects, teachers, students).
 */
export interface PagedResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}

/**
 * Type guard: does this look like an ApiResponse envelope, or a raw payload?
 * Raw JPA entities/DTOs from not-yet-refactored backend packages never have a
 * boolean `success` field, so this is a safe discriminator.
 */
function isApiResponse<T>(value: unknown): value is ApiResponse<T> {
  return (
    typeof value === 'object' &&
    value !== null &&
    'success' in value &&
    typeof (value as { success: unknown }).success === 'boolean'
  )
}

/** Unwraps an ApiResponse envelope if present, otherwise passes the value through untouched. */
export function unwrap<T>(value: T | ApiResponse<T>): T {
  return isApiResponse<T>(value) ? value.data : value
}

/**
 * Type guard: is this a PagedResponse (has a `content` array + pagination metadata),
 * as opposed to a bare array?
 */
function isPagedResponse<T>(value: unknown): value is PagedResponse<T> {
  return (
    typeof value === 'object' &&
    value !== null &&
    Array.isArray((value as { content?: unknown }).content)
  )
}

/**
 * Extracts a plain array from either a PagedResponse or a bare array, so list-fetching
 * functions can present one consistent `T[]` return type to page components regardless
 * of whether that particular backend package has been migrated to pagination yet.
 */
export function unwrapList<T>(value: T[] | PagedResponse<T>): T[] {
  return isPagedResponse<T>(value) ? value.content : value
}
