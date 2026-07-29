package com.collegeerp.Backend.auth;

import com.collegeerp.Backend.auth.dto.LoginResponse;
import com.collegeerp.Backend.common.SuperAdmin;
import com.collegeerp.Backend.common.SuperAdminPasswordResetToken;
import com.collegeerp.Backend.common.SuperAdminPasswordResetTokenRepository;
import com.collegeerp.Backend.common.SuperAdminRepository;
import com.collegeerp.Backend.common.User;
import com.collegeerp.Backend.common.UserRepository;
import com.collegeerp.Backend.common.dto.ApiResponse;
import com.collegeerp.Backend.common.exception.AccountDisabledException;
import com.collegeerp.Backend.common.exception.BadRequestException;
import com.collegeerp.Backend.common.exception.InvalidCredentialsException;
import com.collegeerp.Backend.common.exception.ResourceNotFoundException;
import com.collegeerp.Backend.common.service.EmailService;
import com.collegeerp.Backend.security.JwtService;
import com.collegeerp.Backend.student.entity.Student;
import com.collegeerp.Backend.student.repository.StudentRepository;
import com.collegeerp.Backend.teacher.entity.Teacher;
import com.collegeerp.Backend.teacher.repository.TeacherRepository;
import com.collegeerp.Backend.tenant.TenantContext;
import com.collegeerp.Backend.tenant.entity.Tenant;
import com.collegeerp.Backend.tenant.repository.TenantRepository;
import com.collegeerp.Backend.tenant.service.SubscriptionExpiryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final String STUDENT_ROLE = "STUDENT";
    private static final String TEACHER_ROLE = "TEACHER";
    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";
    private static final String STUDENT_STATUS_ACTIVE = "ACTIVE";
    private static final String PUBLIC_SCHEMA = "public";

    // Account-type tags embedded in refresh tokens only (see JwtService#generateRefreshToken) -
    // tells /api/auth/refresh which repository to re-look-up the account in.
    private static final String ACCOUNT_TYPE_STAFF = "STAFF";
    private static final String ACCOUNT_TYPE_TEACHER = "TEACHER";
    private static final String ACCOUNT_TYPE_STUDENT = "STUDENT";
    private static final String ACCOUNT_TYPE_SUPER_ADMIN = "SUPER_ADMIN";

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SuperAdminRepository superAdminRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final SubscriptionExpiryService subscriptionExpiryService;
    private final PasswordResetService passwordResetService;
    private final SuperAdminPasswordResetTokenRepository superAdminPasswordResetTokenRepository;
    private final EmailService emailService;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.password-reset-token-expiry-minutes}")
    private long passwordResetTokenExpiryMinutes;

    public AuthController(
            TenantRepository tenantRepository,
            UserRepository userRepository,
            StudentRepository studentRepository,
            TeacherRepository teacherRepository,
            SuperAdminRepository superAdminRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            SubscriptionExpiryService subscriptionExpiryService,
            PasswordResetService passwordResetService,
            SuperAdminPasswordResetTokenRepository superAdminPasswordResetTokenRepository,
            EmailService emailService) {

        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.superAdminRepository = superAdminRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.subscriptionExpiryService = subscriptionExpiryService;
        this.passwordResetService = passwordResetService;
        this.superAdminPasswordResetTokenRepository = superAdminPasswordResetTokenRepository;
        this.emailService = emailService;
    }

    @Value("${SUPER_ADMIN_CODE:SUPERADMIN}")
    private String superAdminCode;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {

        // The super admin isn't scoped to any real college, but still types an
        // "institution code" like everyone else - it's just a reserved value
        // (SUPER_ADMIN_CODE env var, default "SUPERADMIN") instead of a real tenant
        // subdomain. Nothing in the UI hints that this value is special.
        if (superAdminCode.equalsIgnoreCase(request.collegeCode().trim())) {
            return ApiResponse.success("Login successful", authenticateSuperAdmin(request.email(), request.password()));
        }

        Tenant tenant = tenantRepository.findBySubdomain(request.collegeCode())
                .orElseThrow(() -> new ResourceNotFoundException("College not found for code: " + request.collegeCode()));

        // Catches a subscription that expired since the last hourly sweep - suspends it
        // right now so the isActive check immediately below sees the up-to-date state.
        subscriptionExpiryService.suspendIfExpired(tenant);

        if (!tenant.isActive()) {
            throw new AccountDisabledException(
                    "This institution's account has been suspended. Please contact the platform administrator.");
        }

        TenantContext.setCurrentTenant(tenant.getSchemaName());

        try {
            LoginResponse response = authenticateStaffOrAdmin(request, tenant)
                    .or(() -> authenticateTeacher(request, tenant))
                    .or(() -> authenticateStudent(request, tenant))
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

            log.info("Successful login for '{}' on tenant '{}'", request.email(), tenant.getSchemaName());
            return ApiResponse.success("Login successful", response);

        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Kept for backward compatibility / direct API use, but the main {@link #login}
     * endpoint now handles this too: sending a blank {@code collegeCode} to
     * {@code POST /api/auth/login} routes here internally, so the frontend doesn't need
     * a separate form for this one role.
     */
    @PostMapping("/super-admin/login")
    public ApiResponse<LoginResponse> superAdminLogin(@Valid @RequestBody SuperAdminLoginRequest request) {
        return ApiResponse.success("Login successful", authenticateSuperAdmin(request.email(), request.password()));
    }

    /**
     * A super admin isn't scoped to any college, so there's no {@code collegeCode} to
     * resolve a tenant from. Authenticates directly against the public-schema
     * {@code super_admins} table and issues a JWT with schema="public" and
     * role=SUPER_ADMIN, which is what {@code SecurityConfig} requires to call
     * {@code POST /api/tenants/register}.
     */
    private LoginResponse authenticateSuperAdmin(String email, String password) {
        SuperAdmin admin = superAdminRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(password, admin.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
        if (!admin.isActive()) {
            throw new AccountDisabledException("This account has been disabled");
        }

        String token = jwtService.generateAccessToken(admin.getId(), admin.getEmail(), PUBLIC_SCHEMA, SUPER_ADMIN_ROLE);
        String refreshToken = jwtService.generateRefreshToken(admin.getId(), admin.getEmail(), PUBLIC_SCHEMA, ACCOUNT_TYPE_SUPER_ADMIN);
        log.info("Successful super admin login for '{}'", admin.getEmail());

        return LoginResponse.of(token, refreshToken, accessTokenExpiration, admin.getEmail(), SUPER_ADMIN_ROLE, PUBLIC_SCHEMA);
    }

    /**
     * Silently renews an access token using a refresh token, without the caller
     * re-sending credentials. This is what lets a session survive past the 15-minute
     * access-token lifetime instead of forcing a full re-login every time it expires -
     * the frontend's axios interceptor calls this once on a 401 and retries the
     * original request, rather than logging the user out immediately.
     * <p>
     * Permissions/role are re-read from the DB here (not copied from the old token), so
     * a role edit made by an admin takes effect on the user's very next silent refresh
     * instead of being stuck until they fully log out and back in.
     */
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {

        String refreshToken = request.refreshToken();

        if (!jwtService.isTokenValid(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
            throw new InvalidCredentialsException("Invalid or expired refresh token");
        }

        String schema = jwtService.extractSchema(refreshToken);
        String accountType = jwtService.extractAccountType(refreshToken);
        Long id = jwtService.extractUserId(refreshToken);

        TenantContext.setCurrentTenant(schema);

        try {
            LoginResponse response = switch (accountType) {
                case ACCOUNT_TYPE_SUPER_ADMIN -> refreshSuperAdmin(id);
                case ACCOUNT_TYPE_STAFF -> refreshStaffOrAdmin(id, schema);
                case ACCOUNT_TYPE_TEACHER -> refreshTeacher(id, schema);
                case ACCOUNT_TYPE_STUDENT -> refreshStudent(id, schema);
                default -> throw new InvalidCredentialsException("Invalid or expired refresh token");
            };

            return ApiResponse.success("Token refreshed", response);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Kicks off the "forgot password" flow. Always returns the same generic success
     * message whether or not {@code email} actually matches an account - the caller
     * must never be able to tell whether a given email is registered from this
     * response alone (user enumeration). The actual reset link is emailed
     * out-of-band; see {@link PasswordResetService#requestReset} and
     * {@link #requestSuperAdminPasswordReset}.
     */
    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {

        if (superAdminCode.equalsIgnoreCase(request.collegeCode().trim())) {
            requestSuperAdminPasswordReset(request.email());
        } else {
            Tenant tenant = tenantRepository.findBySubdomain(request.collegeCode())
                    .orElseThrow(() -> new ResourceNotFoundException("College not found for code: " + request.collegeCode()));

            TenantContext.setCurrentTenant(tenant.getSchemaName());
            try {
                passwordResetService.requestReset(request.collegeCode(), request.email());
            } finally {
                TenantContext.clear();
            }
        }

        return ApiResponse.success(
                "If an account exists for that email, a password reset link has been sent.", null);
    }

    /**
     * Redeems a token minted by {@link #forgotPassword} and sets a new password.
     * {@code collegeCode} tells the backend which schema (or the public schema, for
     * the super admin) to look the token up in - see {@link ResetPasswordRequest}.
     */
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {

        if (superAdminCode.equalsIgnoreCase(request.collegeCode().trim())) {
            resetSuperAdminPassword(request.token(), request.newPassword());
        } else {
            Tenant tenant = tenantRepository.findBySubdomain(request.collegeCode())
                    .orElseThrow(() -> new ResourceNotFoundException("College not found for code: " + request.collegeCode()));

            TenantContext.setCurrentTenant(tenant.getSchemaName());
            try {
                passwordResetService.resetPassword(request.token(), request.newPassword());
            } finally {
                TenantContext.clear();
            }
        }

        return ApiResponse.success("Password reset successful. You can now log in.", null);
    }

    /** Public-schema equivalent of {@link PasswordResetService#requestReset}, for the super admin. */
    private void requestSuperAdminPasswordReset(String email) {
        superAdminRepository.findByEmail(email.trim().toLowerCase()).ifPresent(admin -> {
            String token = generatePasswordResetToken();
            superAdminPasswordResetTokenRepository.save(SuperAdminPasswordResetToken.builder()
                    .token(token)
                    .email(admin.getEmail())
                    .expiresAt(LocalDateTime.now().plusMinutes(passwordResetTokenExpiryMinutes))
                    .used(false)
                    .createdAt(LocalDateTime.now())
                    .build());

            String resetLink = frontendUrl + "/reset-password?token=" + token + "&college=" + superAdminCode;
            try {
                emailService.sendPasswordResetEmail(admin.getEmail(), resetLink, passwordResetTokenExpiryMinutes);
            } catch (Exception e) {
                // Same "log the link instead of failing the request" fallback as
                // PasswordResetService#requestReset - see EmailService's Javadoc.
                log.warn("Could not send password reset email to super admin '{}' - reset link: {}",
                        admin.getEmail(), resetLink, e);
            }
        });
    }

    /** Public-schema equivalent of {@link PasswordResetService#resetPassword}, for the super admin. */
    private void resetSuperAdminPassword(String token, String newPassword) {
        SuperAdminPasswordResetToken resetToken = superAdminPasswordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("This password reset link is invalid or has expired"));

        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("This password reset link is invalid or has expired");
        }

        SuperAdmin admin = superAdminRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new BadRequestException("This password reset link is invalid or has expired"));

        admin.setPasswordHash(passwordEncoder.encode(newPassword));
        superAdminRepository.save(admin);

        resetToken.setUsed(true);
        superAdminPasswordResetTokenRepository.save(resetToken);
    }

    private String generatePasswordResetToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

    private LoginResponse refreshSuperAdmin(Long id) {
        SuperAdmin admin = superAdminRepository.findById(id)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired refresh token"));
        if (!admin.isActive()) {
            throw new AccountDisabledException("This account has been disabled");
        }
        String token = jwtService.generateAccessToken(admin.getId(), admin.getEmail(), PUBLIC_SCHEMA, SUPER_ADMIN_ROLE);
        String newRefreshToken = jwtService.generateRefreshToken(admin.getId(), admin.getEmail(), PUBLIC_SCHEMA, ACCOUNT_TYPE_SUPER_ADMIN);
        return LoginResponse.of(token, newRefreshToken, accessTokenExpiration, admin.getEmail(), SUPER_ADMIN_ROLE, PUBLIC_SCHEMA);
    }

    private LoginResponse refreshStaffOrAdmin(Long id, String schema) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired refresh token"));
        if (!user.isActive()) {
            throw new AccountDisabledException("This account has been disabled");
        }
        String token = jwtService.generateAccessToken(
                user.getId(), user.getEmail(), schema, user.getRole().getName(), user.getRole().getPermissions());
        String newRefreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail(), schema, ACCOUNT_TYPE_STAFF);
        return LoginResponse.ofStaff(token, newRefreshToken, accessTokenExpiration, user.getEmail(), user.getRole().getName(),
                schema, user.isMustChangePassword());
    }

    private LoginResponse refreshTeacher(Long id, String schema) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired refresh token"));
        String token = jwtService.generateAccessToken(teacher.getId(), teacher.getEmail(), schema, TEACHER_ROLE);
        String newRefreshToken = jwtService.generateRefreshToken(teacher.getId(), teacher.getEmail(), schema, ACCOUNT_TYPE_TEACHER);
        return LoginResponse.of(token, newRefreshToken, accessTokenExpiration, teacher.getEmail(), TEACHER_ROLE, schema);
    }

    private LoginResponse refreshStudent(Long id, String schema) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired refresh token"));
        if (!STUDENT_STATUS_ACTIVE.equalsIgnoreCase(student.getStatus())) {
            throw new AccountDisabledException("This student account has been disabled");
        }
        String token = jwtService.generateAccessToken(student.getId(), student.getEmail(), schema, STUDENT_ROLE);
        String newRefreshToken = jwtService.generateRefreshToken(student.getId(), student.getEmail(), schema, ACCOUNT_TYPE_STUDENT);
        return LoginResponse.of(token, newRefreshToken, accessTokenExpiration, student.getEmail(), STUDENT_ROLE, schema);
    }

    private java.util.Optional<LoginResponse> authenticateStaffOrAdmin(LoginRequest request, Tenant tenant) {
        return userRepository.findByEmail(request.email())
                .map(user -> {
                    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                        throw new InvalidCredentialsException("Invalid email or password");
                    }
                    if (!user.isActive()) {
                        throw new AccountDisabledException("This account has been disabled");
                    }
                    String token = jwtService.generateAccessToken(
                            user.getId(), user.getEmail(), tenant.getSchemaName(), user.getRole().getName(),
                            user.getRole().getPermissions());
                    String refreshToken = jwtService.generateRefreshToken(
                            user.getId(), user.getEmail(), tenant.getSchemaName(), ACCOUNT_TYPE_STAFF);
                    return LoginResponse.ofStaff(token, refreshToken, accessTokenExpiration, user.getEmail(), user.getRole().getName(),
                            tenant.getSchemaName(), user.isMustChangePassword());
                });
    }

    private java.util.Optional<LoginResponse> authenticateTeacher(LoginRequest request, Tenant tenant) {
        return teacherRepository.findByEmail(request.email())
                .map(teacher -> {
                    if (teacher.getPasswordHash() == null) {
                        // Teacher record predates the password_hash column being added, or was
                        // created before a password was ever set. Fail clearly instead of
                        // letting passwordEncoder.matches() throw on a null encoded value.
                        throw new InvalidCredentialsException(
                                "This teacher account has no password set. Please contact an administrator.");
                    }
                    if (!passwordEncoder.matches(request.password(), teacher.getPasswordHash())) {
                        throw new InvalidCredentialsException("Invalid email or password");
                    }
                    String token = jwtService.generateAccessToken(
                            teacher.getId(), teacher.getEmail(), tenant.getSchemaName(), TEACHER_ROLE);
                    String refreshToken = jwtService.generateRefreshToken(
                            teacher.getId(), teacher.getEmail(), tenant.getSchemaName(), ACCOUNT_TYPE_TEACHER);
                    return LoginResponse.of(token, refreshToken, accessTokenExpiration, teacher.getEmail(), TEACHER_ROLE, tenant.getSchemaName());
                });
    }

    private java.util.Optional<LoginResponse> authenticateStudent(LoginRequest request, Tenant tenant) {
        return studentRepository.findByEmail(request.email())
                .map(student -> {
                    if (!passwordEncoder.matches(request.password(), student.getPasswordHash())) {
                        throw new InvalidCredentialsException("Invalid email or password");
                    }
                    if (!STUDENT_STATUS_ACTIVE.equalsIgnoreCase(student.getStatus())) {
                        throw new AccountDisabledException("This student account has been disabled");
                    }
                    String token = jwtService.generateAccessToken(
                            student.getId(), student.getEmail(), tenant.getSchemaName(), STUDENT_ROLE);
                    String refreshToken = jwtService.generateRefreshToken(
                            student.getId(), student.getEmail(), tenant.getSchemaName(), ACCOUNT_TYPE_STUDENT);
                    return LoginResponse.of(token, refreshToken, accessTokenExpiration, student.getEmail(), STUDENT_ROLE, tenant.getSchemaName());
                });
    }
}
