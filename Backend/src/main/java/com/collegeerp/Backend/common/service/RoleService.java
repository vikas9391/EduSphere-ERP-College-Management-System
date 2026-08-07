package com.collegeerp.Backend.common.service;

import com.collegeerp.Backend.common.Permission;
import com.collegeerp.Backend.common.Role;
import com.collegeerp.Backend.common.RoleRepository;
import com.collegeerp.Backend.common.User;
import com.collegeerp.Backend.common.UserRepository;
import com.collegeerp.Backend.common.dto.RoleRequest;
import com.collegeerp.Backend.common.dto.RoleResponse;
import com.collegeerp.Backend.common.exception.BadRequestException;
import com.collegeerp.Backend.common.exception.DuplicateResourceException;
import com.collegeerp.Backend.common.exception.ForbiddenException;
import com.collegeerp.Backend.common.exception.ResourceNotFoundException;
import com.collegeerp.Backend.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Custom-role CRUD for a tenant, plus the privilege-escalation guard described in the
 * feature plan: an admin can never create or edit a role - or hand it out via
 * {@link UserService} - with more permissions than the admin's own role currently
 * holds. Permissions are re-read from the acting user's row in the DB rather than
 * trusted from their JWT, so a permission revoked mid-session can't be used to
 * escalate via a stale token.
 */
@Service
@Transactional
public class RoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleService.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public RoleService(RoleRepository roleRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    public RoleResponse createRole(RoleRequest request) {

        String name = request.getName().trim();

        if (roleRepository.existsByName(name)) {
            throw new DuplicateResourceException("A role named '" + name + "' already exists");
        }

        Set<String> permissions = validatePermissions(request.getPermissions());
        guardAgainstEscalation(permissions);

        Role role = Role.builder()
                .name(name)
                .description(request.getDescription())
                .isSystemRole(false)
                .permissions(permissions)
                .build();

        role = roleRepository.save(role);
        log.info("Created role id={} name={} permissions={}", role.getId(), role.getName(), permissions);

        return mapToResponse(role);
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public RoleResponse getRole(Long id) {
        return mapToResponse(findRoleOrThrow(id));
    }

    public RoleResponse updateRole(Long id, RoleRequest request) {

        Role role = findRoleOrThrow(id);

        String name = request.getName().trim();

        // Built-in roles (ADMIN, TEACHER) keep their name locked because other
        // services (e.g. TeacherService) look them up by exact name - but their
        // permissions can still be changed freely.
        if (role.isSystemRole() && !role.getName().equals(name)) {
            throw new ForbiddenException(
                    "'" + role.getName() + "' is a built-in role - its name can't be changed, but its permissions can");
        }

        if (!role.getName().equals(name) && roleRepository.existsByName(name)) {
            throw new DuplicateResourceException("A role named '" + name + "' already exists");
        }

        Set<String> permissions = validatePermissions(request.getPermissions());
        guardAgainstEscalation(permissions);

        role.setName(name);
        role.setDescription(request.getDescription());
        role.setPermissions(permissions);

        role = roleRepository.save(role);
        log.info("Updated role id={} permissions={}", role.getId(), permissions);

        return mapToResponse(role);
    }

    public void deleteRole(Long id) {

        Role role = findRoleOrThrow(id);

        if (role.isSystemRole()) {
            throw new ForbiddenException("'" + role.getName() + "' is a built-in role and can't be deleted");
        }

        // Checked up front so the caller gets a clear, actionable message instead of a
        // generic "still referenced by other data" 409 bubbling up from
        // GlobalExceptionHandler#handleDataIntegrityViolation once the FK constraint on
        // users.role_id trips. That handler is still the safety net for any path that
        // reaches the DB without going through this check.
        long assignedUserCount = userRepository.countByRoleId(id);
        if (assignedUserCount > 0) {
            throw new DuplicateResourceException(
                    "'" + role.getName() + "' is still assigned to " + assignedUserCount
                            + (assignedUserCount == 1 ? " user" : " users")
                            + " - reassign them to a different role before deleting it");
        }

        roleRepository.delete(role);
        log.info("Deleted role id={}", id);
    }

    /** Every {@link Permission} name, grouped by category - powers the role-builder checklist UI. */
    @Transactional(readOnly = true)
    public List<com.collegeerp.Backend.common.dto.PermissionInfo> getAllPermissions() {
        return java.util.Arrays.stream(Permission.values())
                .map(com.collegeerp.Backend.common.dto.PermissionInfo::of)
                .toList();
    }

    private Set<String> validatePermissions(Set<String> requested) {
        if (requested == null) {
            return new java.util.HashSet<>();
        }
        Set<String> valid = java.util.Arrays.stream(Permission.values()).map(Enum::name).collect(Collectors.toSet());
        Set<String> unknown = requested.stream().filter(p -> !valid.contains(p)).collect(Collectors.toSet());
        if (!unknown.isEmpty()) {
            throw new BadRequestException("Unknown permission(s): " + unknown);
        }
        // Must be a mutable collection: this gets assigned straight onto the Role
        // entity's @ElementCollection field, and Hibernate mutates that collection
        // in place (clear() + re-populate) during merge/flush. An immutable Set here
        // (e.g. from Set.of()/Set.copyOf()) throws UnsupportedOperationException the
        // moment Hibernate tries to clear it on update.
        return new java.util.HashSet<>(requested);
    }

    /**
     * An admin can only grant permissions they themselves hold - prevents a
     * lower-privileged admin from crafting a role that has more power than they do and
     * handing it to (or becoming) a more powerful account.
     */
    private void guardAgainstEscalation(Set<String> requestedPermissions) {
        Set<String> actingPermissions = currentUserPermissions();
        if (!actingPermissions.containsAll(requestedPermissions)) {
            Set<String> notHeld = requestedPermissions.stream()
                    .filter(p -> !actingPermissions.contains(p))
                    .collect(Collectors.toSet());
            throw new ForbiddenException(
                    "You can't grant permission(s) you don't have yourself: " + notHeld);
        }
    }

    private Set<String> currentUserPermissions() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            // Defensive - every endpoint that reaches this guard is behind Spring
            // Security auth, so this should be unreachable in practice.
            throw new ForbiddenException("Could not verify the acting user's permissions");
        }
        User actingUser = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ForbiddenException("Could not verify the acting user's permissions"));
        return actingUser.getRole().getPermissions();
    }

    private Role findRoleOrThrow(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Role", id));
    }

    private RoleResponse mapToResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .isSystemRole(role.isSystemRole())
                .permissions(role.getPermissions())
                .build();
    }
}