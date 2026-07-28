package com.collegeerp.Backend.common;

/**
 * Fixed, code-defined set of granular permissions a {@link Role} can be made up of.
 * Deliberately not admin-defined free text - keeping this an enum means every
 * permission referenced by a {@code @PreAuthorize("hasAuthority(...)")} check
 * somewhere in the codebase is guaranteed to exist, and new permissions only show up
 * in the Role-builder UI once someone has actually wired up the check they gate.
 * <p>
 * Follows an {@code ACTION_RESOURCE} naming convention so view-only roles (e.g. a
 * "Supervisor" who can see reports but create/edit/delete nothing) are possible.
 * {@link #category} groups permissions for the frontend role-builder screen only -
 * it has no bearing on authorization itself.
 */
public enum Permission {

    CREATE_TEACHER("Teachers"),
    EDIT_TEACHER("Teachers"),
    DELETE_TEACHER("Teachers"),
    VIEW_TEACHER("Teachers"),
    VIEW_TEACHER_PROGRESS("Teachers"),

    CREATE_STUDENT("Students"),
    EDIT_STUDENT("Students"),
    DELETE_STUDENT("Students"),
    VIEW_STUDENT("Students"),

    CREATE_DEPARTMENT("Academics"),
    EDIT_DEPARTMENT("Academics"),
    DELETE_DEPARTMENT("Academics"),
    VIEW_DEPARTMENT("Academics"),

    CREATE_COURSE("Academics"),
    EDIT_COURSE("Academics"),
    DELETE_COURSE("Academics"),
    VIEW_COURSE("Academics"),

    CREATE_SUBJECT("Academics"),
    EDIT_SUBJECT("Academics"),
    DELETE_SUBJECT("Academics"),
    VIEW_SUBJECT("Academics"),

    MANAGE_ENROLLMENT("Enrollment & Attendance"),
    VIEW_ENROLLMENT("Enrollment & Attendance"),
    MANAGE_ATTENDANCE("Enrollment & Attendance"),
    VIEW_ATTENDANCE_REPORTS("Enrollment & Attendance"),

    MANAGE_ASSIGNMENTS("Assessments"),
    VIEW_ASSIGNMENTS("Assessments"),
    MANAGE_EXAMS("Assessments"),
    MANAGE_MARKS("Assessments"),
    VIEW_RESULTS("Assessments"),

    CREATE_ROLE("Administration"),
    EDIT_ROLE("Administration"),
    DELETE_ROLE("Administration"),
    ASSIGN_ROLE("Administration"),

    CREATE_USER("Administration"),
    EDIT_USER("Administration"),
    DELETE_USER("Administration"),
    VIEW_USER("Administration");

    private final String category;

    Permission(String category) {
        this.category = category;
    }

    public String getCategory() {
        return category;
    }
}
