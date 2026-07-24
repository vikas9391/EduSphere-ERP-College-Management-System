import { api } from './axios'

// Mirrors com.collegeerp.Backend.tenant.dto.TenantRegistrationRequest
export interface TenantRegisterPayload {
  collegeName: string
  subdomain: string
  adminEmail: string
  password: string
}

// Mirrors com.collegeerp.Backend.tenant.dto.TenantRegistrationResponse
export interface TenantRegistrationResponse {
  tenantId: string // UUID
  collegeName: string
  schemaName: string
  message: string
}

// Backend route: @RequestMapping("/api/tenants") + @PostMapping("/register")
// Requires a SUPER_ADMIN-authenticated caller — see SecurityConfig on the backend.
export async function registerTenant(
  payload: TenantRegisterPayload
): Promise<TenantRegistrationResponse> {
  const res = await api.post<TenantRegistrationResponse>('/tenants/register', payload)
  return res.data
}

// Mirrors com.collegeerp.Backend.tenant.dto.TenantSummaryResponse
export interface TenantSummary {
  tenantId: string // UUID
  collegeName: string
  subdomain: string
  schemaName: string
  isActive: boolean
  subscriptionPlan: string
  subscriptionStatus: 'ACTIVE' | 'EXPIRED' | 'CANCELLED'
  subscriptionExpiresAt: string | null
  createdAt: string
}

// Backend route: @RequestMapping("/api/tenants") + @GetMapping — also SUPER_ADMIN only.
export async function listTenants(): Promise<TenantSummary[]> {
  const res = await api.get<TenantSummary[]>('/tenants')
  return res.data
}

// Backend route: PATCH /api/tenants/{id}/status
// Suspending blocks every login under that college (admin, teacher, student) until
// reactivated. Reversible - no data is touched.
export async function updateTenantStatus(tenantId: string, isActive: boolean): Promise<TenantSummary> {
  const res = await api.patch<TenantSummary>(`/tenants/${tenantId}/status`, { isActive })
  return res.data
}

// Mirrors com.collegeerp.Backend.tenant.dto.TenantSubscriptionUpdateRequest
export interface TenantSubscriptionUpdatePayload {
  plan: string
  status: 'ACTIVE' | 'EXPIRED' | 'CANCELLED'
  expiresAt: string | null // ISO datetime, or null for no expiry
}

// Backend route: PATCH /api/tenants/{id}/subscription
// Bookkeeping only - does not itself block logins. Suspend via updateTenantStatus too
// if an expired/cancelled subscription should actually lock the college out.
export async function updateTenantSubscription(
  tenantId: string,
  payload: TenantSubscriptionUpdatePayload
): Promise<TenantSummary> {
  const res = await api.patch<TenantSummary>(`/tenants/${tenantId}/subscription`, payload)
  return res.data
}

// Backend route: DELETE /api/tenants/{id}
// Irreversible - drops the college's entire schema and every record in it. The caller
// is responsible for getting explicit confirmation before calling this.
export async function deleteTenant(tenantId: string): Promise<void> {
  await api.delete(`/tenants/${tenantId}`)
}

// Mirrors com.collegeerp.Backend.tenant.dto.TenantDetailsResponse
export interface TenantDetails {
  college: TenantSummary
  adminStaffCount: number
  teacherCount: number
  studentCount: number
  departmentCount: number
  courseCount: number
  subjectCount: number
  enrollmentCount: number
}

// Backend route: GET /api/tenants/{id}/details
// Live counts read from that college's own schema - not cached, so this can be a bit
// slower than the plain list. Works even for a suspended college.
export async function getTenantDetails(tenantId: string): Promise<TenantDetails> {
  const res = await api.get<TenantDetails>(`/tenants/${tenantId}/details`)
  return res.data
}