package com.collegeerp.Backend.auth;

import com.collegeerp.Backend.common.PasswordResetToken;
import com.collegeerp.Backend.common.PasswordResetTokenRepository;
import com.collegeerp.Backend.common.User;
import com.collegeerp.Backend.common.UserRepository;
import com.collegeerp.Backend.common.exception.BadRequestException;
import com.collegeerp.Backend.common.service.EmailService;
import com.collegeerp.Backend.student.entity.Student;
import com.collegeerp.Backend.student.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Handles "forgot password" for tenant-scoped accounts: staff/admin/teacher (all
 * {@link User} rows - teacher credentials moved onto the shared users/roles table, see
 * {@code TeacherService}) and {@link Student}. Every method here must be called with
 * {@link com.collegeerp.Backend.tenant.TenantContext} already set to the right
 * schema - see {@code AuthController#forgotPassword}/{@code #resetPassword}, which
 * set/clear it the same way {@code AuthController#login} does.
 * <p>
 * The super admin's equivalent flow lives directly in {@code AuthController}
 * instead, since its account and reset tokens are pinned to the public schema and
 * never need a TenantContext at all (mirrors how {@code authenticateSuperAdmin}
 * bypasses TenantContext today).
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private static final String ACCOUNT_TYPE_STAFF = "STAFF";
    private static final String ACCOUNT_TYPE_STUDENT = "STUDENT";

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.password-reset-token-expiry-minutes}")
    private long tokenExpiryMinutes;

    public PasswordResetService(
            PasswordResetTokenRepository tokenRepository,
            UserRepository userRepository,
            StudentRepository studentRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * Always "succeeds" from the caller's point of view whether or not {@code email}
     * actually matches an account in this tenant - {@code AuthController} returns the
     * same generic message either way, so a caller can never use this endpoint to
     * enumerate which emails are registered. If nothing matches, this is a silent
     * no-op after the lookup.
     */
    public void requestReset(String collegeCode, String email) {
        String accountType = resolveAccountType(email);
        if (accountType == null) {
            log.info("Password reset requested for an email with no matching account in this tenant - ignoring");
            return;
        }

        String token = generateToken();
        tokenRepository.save(PasswordResetToken.builder()
                .token(token)
                .email(email)
                .accountType(accountType)
                .expiresAt(LocalDateTime.now().plusMinutes(tokenExpiryMinutes))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build());

        String resetLink = frontendUrl + "/reset-password?token=" + token + "&college=" + collegeCode;

        try {
            emailService.sendPasswordResetEmail(email, resetLink, tokenExpiryMinutes);
        } catch (Exception e) {
            // No SMTP configured in this environment (or the send otherwise failed) -
            // don't fail the request over it, but surface the link in the logs so
            // local/dev testing still works end to end. See EmailService's Javadoc.
            log.warn("Could not send password reset email to '{}' - reset link: {}", email, resetLink, e);
        }
    }

    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("This password reset link is invalid or has expired"));

        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("This password reset link is invalid or has expired");
        }

        String encoded = passwordEncoder.encode(newPassword);

        switch (resetToken.getAccountType()) {
            case ACCOUNT_TYPE_STAFF -> {
                User user = userRepository.findByEmail(resetToken.getEmail())
                        .orElseThrow(() -> new BadRequestException("This password reset link is invalid or has expired"));
                user.setPasswordHash(encoded);
                // Matches UserService#changePassword: once the account holder sets their
                // own password (even via a reset link) they shouldn't be forced through
                // ChangePasswordPage again.
                user.setMustChangePassword(false);
                userRepository.save(user);
            }
            case ACCOUNT_TYPE_STUDENT -> {
                Student student = studentRepository.findByEmail(resetToken.getEmail())
                        .orElseThrow(() -> new BadRequestException("This password reset link is invalid or has expired"));
                student.setPasswordHash(encoded);
                studentRepository.save(student);
            }
            default -> throw new BadRequestException("This password reset link is invalid or has expired");
        }

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }

    /**
     * Same lookup order AuthController#login tries accounts in: staff/admin/teacher
     * (all live in "users" now), then student.
     */
    private String resolveAccountType(String email) {
        if (userRepository.findByEmail(email).isPresent()) return ACCOUNT_TYPE_STAFF;
        if (studentRepository.findByEmail(email).isPresent()) return ACCOUNT_TYPE_STUDENT;
        return null;
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
    }
}
