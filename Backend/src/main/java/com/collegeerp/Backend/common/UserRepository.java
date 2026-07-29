package com.collegeerp.Backend.common;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    /**
     * Resolves to a {@code role.id} traversal (User has no direct roleId field) - used
     * by {@code RoleService#deleteRole} to give a clean "still assigned to N users"
     * error instead of leaning on the FK violation / generic 409 from
     * {@code GlobalExceptionHandler#handleDataIntegrityViolation}.
     */
    long countByRoleId(Long roleId);
}
