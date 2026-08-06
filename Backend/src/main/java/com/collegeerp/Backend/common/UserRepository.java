package com.collegeerp.Backend.common;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    /** Teacher-only column - always null for non-teacher rows, so this is effectively "is this employee ID taken by a teacher". */
    boolean existsByEmployeeId(String employeeId);

    /** Backs TeacherService's list/CRUD - a "teacher" is just a User row with role.name = "TEACHER". */
    Page<User> findAllByRole_Name(String roleName, Pageable pageable);

    long countByRole_Name(String roleName);

    /**
     * Resolves to a {@code role.id} traversal (User has no direct roleId field) - used
     * by {@code RoleService#deleteRole} to give a clean "still assigned to N users"
     * error instead of leaning on the FK violation / generic 409 from
     * {@code GlobalExceptionHandler#handleDataIntegrityViolation}.
     */
    long countByRoleId(Long roleId);
}
