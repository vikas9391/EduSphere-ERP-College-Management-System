package com.collegeerp.Backend.tenant.service;

import java.sql.Connection;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Component;

import com.collegeerp.Backend.common.exception.TenantProvisioningException;

/**
 * Creates (if needed) and Flyway-migrates a single tenant schema. Extracted from
 * {@link TenantProvisioningService} so the exact same logic can be reused by
 * {@link TenantSchemaStartupMigrator}, which re-runs it against every EXISTING tenant
 * schema on application startup.
 * <p>
 * Idempotent and safe to call repeatedly against the same schema: {@code CREATE SCHEMA
 * IF NOT EXISTS} is a no-op if the schema already exists, and Flyway's own migration
 * history table tracks which versions have already been applied to that schema, so
 * {@code migrate()} only ever applies whatever migrations are new since the last run -
 * this is exactly what closes the gap where a schema created before a new tenant-migration
 * file existed (e.g. V15 adding teacher passwords) would otherwise never receive it.
 */
@Component
public class TenantSchemaMigrator {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public TenantSchemaMigrator(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    public void migrateSchema(String schemaName) {
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS \"" + schemaName + "\"");

        try (Connection connection = dataSource.getConnection()) {

            // Flyway must not close the pooled connection it borrows.
            SingleConnectionDataSource flywayDataSource = new SingleConnectionDataSource(connection, true);

            Flyway.configure()
                    .dataSource(flywayDataSource)
                    .schemas(schemaName)
                    .locations("classpath:db/tenant-migration")
                    .load()
                    .migrate();

            try (var statement = connection.createStatement()) {
                statement.execute("SET search_path TO public");
            }

        } catch (Exception e) {
            throw new TenantProvisioningException("Tenant schema migration failed for schema: " + schemaName, e);
        }
    }

    /**
     * Irreversibly drops a tenant's entire schema - every student, teacher, course, and
     * record that college ever had. {@code CASCADE} removes all tables/data inside it
     * without needing to know their names or drop order. There is no undo short of a
     * database backup; callers (see {@code TenantProvisioningService#deleteTenant}) must
     * only reach this after explicit super-admin confirmation.
     */
    public void dropSchema(String schemaName) {
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
    }
}