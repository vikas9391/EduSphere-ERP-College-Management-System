package com.collegeerp.Backend.marks.service;

public final class GradeUtil {

    private GradeUtil() {
    }

    public static String gradeFor(double percentage) {

        if (percentage >= 90) return "O";
        if (percentage >= 80) return "A+";
        if (percentage >= 70) return "A";
        if (percentage >= 60) return "B+";
        if (percentage >= 50) return "B";
        if (percentage >= 40) return "C";

        return "F";
    }

    public static double gradePointFor(String grade) {

        return switch (grade) {
            case "O" -> 10.0;
            case "A+" -> 9.0;
            case "A" -> 8.0;
            case "B+" -> 7.0;
            case "B" -> 6.0;
            case "C" -> 5.0;
            default -> 0.0;
        };
    }
}
