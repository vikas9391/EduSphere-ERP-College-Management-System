package com.collegeerp.Backend.attendance.service;

import java.util.Locale;

/**
 * Canonical attendance calculation policy.
 * PRESENT and LATE count as attended. ABSENT counts as missed. EXCUSED is excluded from the
 * attendance denominator. ATTENDED is accepted as a legacy alias for PRESENT during migration.
 */
public final class AttendanceStatusPolicy {

    private AttendanceStatusPolicy() {}

    public static boolean isAttended(String status) {
        String normalized = normalize(status);
        return "PRESENT".equals(normalized)
                || "LATE".equals(normalized)
                || "ATTENDED".equals(normalized);
    }

    public static boolean countsTowardPercentage(String status) {
        return !"EXCUSED".equals(normalize(status));
    }

    private static String normalize(String status) {
        return status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
    }
}
