ALTER TABLE tenants
    ADD COLUMN subscription_plan VARCHAR(50) NOT NULL DEFAULT 'TRIAL',
    ADD COLUMN subscription_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN subscription_expires_at TIMESTAMP NULL;
