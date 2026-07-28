package com.collegeerp.Backend.common.dto;

import com.collegeerp.Backend.common.Permission;

/** {@code {"name": "VIEW_TEACHER_PROGRESS", "category": "Teachers"}} - the bare
 *  {@link Permission} enum serializes as just its name via Jackson by default, which
 *  would lose the category grouping the role-builder UI needs. */
public record PermissionInfo(String name, String category) {
    public static PermissionInfo of(Permission permission) {
        return new PermissionInfo(permission.name(), permission.getCategory());
    }
}
