package com.collegeerp.Backend.config;

import com.collegeerp.Backend.common.SuperAdmin;
import com.collegeerp.Backend.common.SuperAdminRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Bootstraps the first super admin account on startup, from SUPER_ADMIN_EMAIL /
 * SUPER_ADMIN_PASSWORD env vars, if no super admin with that email exists yet.
 * <p>
 * This exists to solve the chicken-and-egg problem: college registration
 * (POST /api/tenants/register) now requires a SUPER_ADMIN-authenticated caller, and
 * there is deliberately no self-registration endpoint for super admins (see
 * {@code SecurityConfig}), so the very first one has to come from somewhere other than
 * the API. Re-running with the same env vars is a no-op once that account exists —
 * this never overwrites an existing password, so rotating SUPER_ADMIN_PASSWORD in the
 * environment later does nothing on its own.
 */
@Component
public class SuperAdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminSeeder.class);

    private final SuperAdminRepository superAdminRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${SUPER_ADMIN_EMAIL:}")
    private String seedEmail;

    @Value("${SUPER_ADMIN_PASSWORD:}")
    private String seedPassword;

    public SuperAdminSeeder(SuperAdminRepository superAdminRepository, PasswordEncoder passwordEncoder) {
        this.superAdminRepository = superAdminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (seedEmail == null || seedEmail.isBlank() || seedPassword == null || seedPassword.isBlank()) {
            log.info("SUPER_ADMIN_EMAIL / SUPER_ADMIN_PASSWORD not set — skipping super admin seeding. " +
                    "Set both env vars and restart to create the first super admin account.");
            return;
        }

        String email = seedEmail.trim().toLowerCase();

        if (superAdminRepository.findByEmail(email).isPresent()) {
            log.debug("Super admin '{}' already exists — skipping seeding", email);
            return;
        }

        SuperAdmin admin = SuperAdmin.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(seedPassword))
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        superAdminRepository.save(admin);
        log.info("Seeded initial super admin account for '{}'", email);
    }
}
