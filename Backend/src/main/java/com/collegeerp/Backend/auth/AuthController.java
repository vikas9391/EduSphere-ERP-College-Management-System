package com.collegeerp.Backend.auth;

import com.collegeerp.Backend.auth.dto.LoginResponse;
import com.collegeerp.Backend.common.SuperAdmin;
import com.collegeerp.Backend.common.SuperAdminRepository;
import com.collegeerp.Backend.common.User;
import com.collegeerp.Backend.common.UserRepository;
import com.collegeerp.Backend.common.dto.ApiResponse;
import com.collegeerp.Backend.common.exception.AccountDisabledException;
import com.collegeerp.Backend.common.exception.InvalidCredentialsException;
import com.collegeerp.Backend.common.exception.ResourceNotFoundException;
import com.collegeerp.Backend.security.JwtService;
import com.collegeerp.Backend.student.entity.Student;
import com.collegeerp.Backend.student.repository.StudentRepository;
import com.collegeerp.Backend.teacher.entity.Teacher;
import com.collegeerp.Backend.teacher.repository.TeacherRepository;
import com.collegeerp.Backend.tenant.TenantContext;
import com.collegeerp.Backend.tenant.entity.Tenant;
import com.collegeerp.Backend.tenant.repository.TenantRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final String STUDENT_ROLE = "STUDENT";
    private static final String TEACHER_ROLE = "TEACHER";
    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";
    private static final String STUDENT_STATUS_ACTIVE = "ACTIVE";
    private static final String PUBLIC_SCHEMA = "public";

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SuperAdminRepository superAdminRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    public AuthController(
            TenantRepository tenantRepository,
            UserRepository userRepository,
            StudentRepository studentRepository,
            TeacherRepository teacherRepository,
            SuperAdminRepository superAdminRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder) {

        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.superAdminRepository = superAdminRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
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
        log.info("Successful super admin login for '{}'", admin.getEmail());

        return LoginResponse.of(token, accessTokenExpiration, admin.getEmail(), SUPER_ADMIN_ROLE, PUBLIC_SCHEMA);
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
                            user.getId(), user.getEmail(), tenant.getSchemaName(), user.getRole().getName());
                    return LoginResponse.of(token, accessTokenExpiration, user.getEmail(), user.getRole().getName(), tenant.getSchemaName());
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
                    return LoginResponse.of(token, accessTokenExpiration, teacher.getEmail(), TEACHER_ROLE, tenant.getSchemaName());
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
                    return LoginResponse.of(token, accessTokenExpiration, student.getEmail(), STUDENT_ROLE, tenant.getSchemaName());
                });
    }
}
