package io.sqm.catalog.model;

/**
 * Simplified catalog column type used by schema providers.
 */
public enum CatalogType {
    /**
     * Unknown or unmapped type.
     */
    UNKNOWN,
    /**
     * Text-like values.
     */
    STRING,
    /**
     * Integer-like values.
     */
    INTEGER,
    /**
     * Long integer values.
     */
    LONG,
    /**
     * Decimal or floating-point values.
     */
    DECIMAL,
    /**
     * Boolean values.
     */
    BOOLEAN,
    /**
     * UUID values.
     */
    UUID,
    /**
     * JSON values.
     */
    JSON,
    /**
     * JSONB values.
     */
    JSONB,
    /**
     * Binary values.
     */
    BYTES,
    /**
     * Enum-like values.
     */
    ENUM,
    /**
     * Date values.
     */
    DATE,
    /**
     * Time values.
     */
    TIME,
    /**
     * Timestamp values.
     */
    TIMESTAMP
}
