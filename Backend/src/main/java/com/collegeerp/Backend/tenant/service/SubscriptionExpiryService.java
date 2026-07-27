package com.collegeerp.Backend.tenant.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.collegeerp.Backend.tenant.entity.Tenant;
import com.collegeerp.Backend.tenant.repository.TenantRepository;

/**
 * Enforces subscription expiry. Setting {@code subscriptionExpiresAt} to a past date via
 * {@code PATCH /api/tenants/{id}/subscription} used to be bookkeeping-only - it recorded
 * the date but nothing ever checked it, so an "expired" tenant stayed fully logged-in-able
 * until a super admin manually hit Suspend. This closes that gap two ways:
 * <p>
 * 1. {@link #suspendIfExpired(Tenant)} - called from {@code AuthController#login} on every
 *    login attempt, before the {@code isActive} check. If the subscription has expired but
 *    the tenant hasn't been flagged inactive yet, it's suspended right there, so the very
 *    next line's {@code isActive} check sees the up-to-date state. This means enforcement
 *    doesn't depend on the sweep below having run yet.
 * <p>
 * 2. {@link #sweepExpiredSubscriptions()} - runs hourly, catches every active tenant whose
 *    subscription has expired even if nobody has tried to log in since - so the super admin
 *    dashboard's Active/Suspended counts and badges reflect reality on their own, without
 *    needing a login attempt to trigger the flip.
 * <p>
 * Both paths do the same thing: flip {@code isActive} to false and {@code subscriptionStatus}
 * to {@code EXPIRED}. This is intentionally the same mechanism as manual Suspend
 * ({@code TenantProvisioningService#updateStatus}) - reversible, no data touched - a super
 * admin can reactivate it (e.g. after the college renews) the same way either case.
 */
@Service
public class SubscriptionExpiryService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionExpiryService.class);
    private static final String EXPIRED_STATUS = "EXPIRED";

    private final TenantRepository tenantRepository;

    public SubscriptionExpiryService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    /**
     * Just-in-time check for a single tenant, meant to run right before a login's
     * {@code isActive} check. No-op if the tenant is already suspended, has no expiry set,
     * or hasn't expired yet. Returns true if this call just suspended it.
     */
    public boolean suspendIfExpired(Tenant tenant) {
        if (!tenant.isActive()) {
            return false;
        }
        if (tenant.getSubscriptionExpiresAt() == null) {
            return false;
        }
        if (tenant.getSubscriptionExpiresAt().isAfter(LocalDateTime.now())) {
            return false;
        }

        tenant.setActive(false);
        tenant.setSubscriptionStatus(EXPIRED_STATUS);
        tenantRepository.save(tenant);
        log.warn("Tenant '{}' (schema={}) auto-suspended at login: subscription expired at {}",
                tenant.getName(), tenant.getSchemaName(), tenant.getSubscriptionExpiresAt());
        return true;
    }

    /**
     * Periodic catch-all so expiry is enforced even for tenants nobody has tried to log
     * into since expiring. Runs on the hour.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void sweepExpiredSubscriptions() {
        List<Tenant> expired = tenantRepository.findByIsActiveTrueAndSubscriptionExpiresAtBefore(LocalDateTime.now());
        if (expired.isEmpty()) {
            return;
        }

        for (Tenant tenant : expired) {
            tenant.setActive(false);
            tenant.setSubscriptionStatus(EXPIRED_STATUS);
        }
        tenantRepository.saveAll(expired);

        log.warn("Subscription sweep auto-suspended {} tenant(s): {}",
                expired.size(),
                expired.stream().map(Tenant::getSchemaName).toList());
    }
}
