package com.collegeerp.Backend.common.service;

import com.collegeerp.Backend.common.Role;
import com.collegeerp.Backend.common.RoleRepository;
import com.collegeerp.Backend.common.User;
import com.collegeerp.Backend.common.UserRepository;
import com.collegeerp.Backend.common.dto.PasswordChangeRequest;
import com.collegeerp.Backend.common.dto.UserCreateRequest;
import com.collegeerp.Backend.common.dto.UserResponse;
import com.collegeerp.Backend.common.exception.BadRequestException;
import com.collegeerp.Backend.common.exception.DuplicateResourceException;
import com.collegeerp.Backend.common.exception.ForbiddenException;
import com.collegeerp.Backend.common.exception.InvalidCredentialsException;
import com.collegeerp.Backend.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * General-purpose staff/admin user creation - replaces the old teacher-only account
 * creation path with "pick a role, any role" (HOD, Supervisor, Accountant, Librarian,
 * or a custom role an admin built in {@link RoleService}). Teacher and Student remain
 * separate domain entities with their own login path (see {@code AuthController}) -
 * this only governs accounts that authenticate through the {@code User}/{@code Role} table.
 */
@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse createUser(UserCreateRequest request, Long actingUserId) {

        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("A user with email '" + email + "' already exists");
        }

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> ResourceNotFoundException.of("Role", request.getRoleId()));

        guardAgainstEscalation(actingUserId, role.getPermissions());

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .role(role)
                .isActive(true)
                .isEmailVerified(false)
                // The admin picked this password for them, not the user themselves -
                // force a change before they can use the account for anything.
                .mustChangePassword(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        user = userRepository.save(user);
        log.info("Created user id={} email={} role={}", user.getId(), user.getEmail(), role.getName());

        return mapToResponse(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        return mapToResponse(findUserOrThrow(id));
    }

    /**
     * Self-service password change - identical for every role (Admin, HOD, Supervisor,
     * whoever). Verifies the current password before allowing the change, same BCrypt
     * flow as account creation, and clears {@code mustChangePassword}.
     */
    public void changePassword(Long userId, PasswordChangeRequest request) {

        User user = findUserOrThrow(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must be different from the current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("User id={} changed their password", userId);
    }

    private void guardAgainstEscalation(Long actingUserId, Set<String> roleBeingAssigned) {
        User actingUser = findUserOrThrow(actingUserId);
        Set<String> actingPermissions = actingUser.getRole().getPermissions();
        if (!actingPermissions.containsAll(roleBeingAssigned)) {
            throw new ForbiddenException(
                    "You can't assign a role with permissions you don't have yourself");
        }
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .roleId(user.getRole().getId())
                .roleName(user.getRole().getName())
                .isActive(user.isActive())
                .mustChangePassword(user.isMustChangePassword())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
