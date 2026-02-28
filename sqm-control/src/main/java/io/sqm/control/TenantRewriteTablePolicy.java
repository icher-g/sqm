package io.sqm.control;

import java.util.Objects;

/**
 * Tenant rewrite policy for a single table mapping.
 *
 * @param tenantColumn tenant column name used for predicate injection.
 * @param mode per-table rewrite behavior.
 */
public record TenantRewriteTablePolicy(String tenantColumn, TenantRewriteTableMode mode) {

    /**
     * Validates and normalizes policy fields.
     *
     * @param tenantColumn tenant column name used for predicate injection.
     * @param mode per-table rewrite behavior.
     */
    public TenantRewriteTablePolicy {
        if (tenantColumn == null || tenantColumn.isBlank()) {
            throw new IllegalArgumentException("tenantColumn must not be blank");
        }
        tenantColumn = tenantColumn.trim();
        mode = Objects.requireNonNullElse(mode, TenantRewriteTableMode.REQUIRED);
    }

    /**
     * Creates a required-tenant policy for the given column.
     *
     * @param tenantColumn tenant column name.
     * @return table policy.
     */
    public static TenantRewriteTablePolicy required(String tenantColumn) {
        return new TenantRewriteTablePolicy(tenantColumn, TenantRewriteTableMode.REQUIRED);
    }

    /**
     * Creates a tenant policy for the given column and mode.
     *
     * @param tenantColumn tenant column name.
     * @param mode table rewrite mode.
     * @return table policy.
     */
    public static TenantRewriteTablePolicy of(String tenantColumn, TenantRewriteTableMode mode) {
        return new TenantRewriteTablePolicy(tenantColumn, mode);
    }
}
