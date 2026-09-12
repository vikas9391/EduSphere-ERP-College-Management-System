package com.collegeerp.Backend.attendance.service;

import java.util.Locale;

/** Canonical attendance calculation policy. */
public final class AttendanceStatusPolicy {

    private AttendanceStatusPolicy() {}

    public static boolean isAttended(String status) {
        String normalized = normalize(status);
        return "PRESENT".equals(normalized) || "LATE".equals(normalized);
    }

    public static boolean countsTowardPercentage(String status) {
        String normalized = normalize(status);
        return !"EXCUSED".equals(normalized) && !"HOLIDAY".equals(normalized);
    }

    private static String normalize(String status) {
        return status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
    }
}
