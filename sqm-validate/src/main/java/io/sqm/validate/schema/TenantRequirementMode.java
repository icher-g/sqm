package io.sqm.validate.schema;

/**
 * Defines behavior when execution context does not provide tenant identifier.
 */
public enum TenantRequirementMode {
    /**
     * Tenant identifier is optional.
     */
    OPTIONAL,

    /**
     * Tenant identifier is required and validation must deny when missing.
     */
    REQUIRED
}

