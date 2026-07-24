package com.collegeerp.Backend.tenant.dto;

/**
 * Response for {@code GET /api/tenants/{id}/details} — the college's own info plus a
 * snapshot of how much it's actually being used. Counts are read live from that
 * college's own schema (see {@code TenantStatsService}), so they're always current as
 * of the request, not cached.
 */
public record TenantDetailsResponse(
        TenantSummaryResponse college,
        long adminStaffCount,
        long teacherCount,
        long studentCount,
        long departmentCount,
        long courseCount,
        long subjectCount,
        long enrollmentCount
) {}
