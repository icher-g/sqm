package io.sqm.validate.schema.model;

import io.sqm.catalog.model.CatalogType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Optional;

/**
 * Represents a simplified database column type used by semantic validation.
 */
public enum DbType {
    /**
     * Unknown or unmapped database type.
     *
     * <p>This type is used when schema extraction cannot map a database-native type
     * to a known semantic type. Validators should treat it conservatively and avoid
     * strict mismatch errors based only on UNKNOWN.</p>
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
     * Binary/blob values.
     */
    BYTES,
    /**
     * Enumerated values.
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
    TIMESTAMP;

    /**
     * Infers a validation type from a literal object.
     *
     * @param value literal value.
     * @return inferred type if recognized.
     */
    public static Optional<DbType> fromLiteral(Object value) {
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof String || value instanceof Character) {
            return Optional.of(STRING);
        }
        if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
            return Optional.of(INTEGER);
        }
        if (value instanceof Long) {
            return Optional.of(LONG);
        }
        if (value instanceof Float || value instanceof Double) {
            return Optional.of(DECIMAL);
        }
        if (value instanceof Boolean) {
            return Optional.of(BOOLEAN);
        }
        if (value instanceof LocalDate) {
            return Optional.of(DATE);
        }
        if (value instanceof LocalTime) {
            return Optional.of(TIME);
        }
        if (value instanceof LocalDateTime) {
            return Optional.of(TIMESTAMP);
        }
        return Optional.empty();
    }

    /**
     * Maps SQL type token to validator type.
     *
     * <p>Unknown or unsupported tokens map to {@link #UNKNOWN}.</p>
     *
     * @param sqlTypeName raw SQL type token.
     * @return mapped validator type.
     */
    public static DbType fromSqlType(String sqlTypeName) {
        if (sqlTypeName == null) {
            return UNKNOWN;
        }
        return switch (sqlTypeName.toLowerCase(Locale.ROOT)) {
            case "smallint", "int", "integer", "int2", "int4" -> INTEGER;
            case "bigint", "int8" -> LONG;
            case "decimal", "numeric", "real", "double", "float", "float4", "float8" -> DECIMAL;
            case "boolean", "bool" -> BOOLEAN;
            case "char", "character", "varchar", "text", "string", "bpchar", "citext" -> STRING;
            case "uuid" -> UUID;
            case "json" -> JSON;
            case "jsonb" -> JSONB;
            case "bytea", "blob", "binary", "varbinary" -> BYTES;
            case "date" -> DATE;
            case "time", "timetz" -> TIME;
            case "timestamp", "timestamptz" -> TIMESTAMP;
            default -> UNKNOWN;
        };
    }

    /**
     * Maps catalog type to validator type.
     *
     * @param catalogType catalog type.
     * @return mapped validator type.
     */
    public static DbType fromCatalogType(CatalogType catalogType) {
        if (catalogType == null) {
            return UNKNOWN;
        }
        return switch (catalogType) {
            case UNKNOWN -> UNKNOWN;
            case STRING -> STRING;
            case INTEGER -> INTEGER;
            case LONG -> LONG;
            case DECIMAL -> DECIMAL;
            case BOOLEAN -> BOOLEAN;
            case UUID -> UUID;
            case JSON -> JSON;
            case JSONB -> JSONB;
            case BYTES -> BYTES;
            case ENUM -> ENUM;
            case DATE -> DATE;
            case TIME -> TIME;
            case TIMESTAMP -> TIMESTAMP;
        };
    }

    /**
     * Checks whether two types can be compared in a binary comparison.
     *
     * @param left left type.
     * @param right right type.
     * @return {@code true} if comparison is allowed.
     */
    public static boolean comparable(DbType left, DbType right) {
        if (!isKnown(left) || !isKnown(right)) {
            return true;
        }
        if (left == right) {
            return true;
        }
        return isNumeric(left) && isNumeric(right);
    }

    /**
     * Indicates whether type is known (not UNKNOWN).
     *
     * @param type type to check.
     * @return true when type is known.
     */
    public static boolean isKnown(DbType type) {
        return type != UNKNOWN;
    }

    /**
     * Indicates whether type is numeric.
     *
     * @param type type to check.
     * @return true for numeric types.
     */
    public static boolean isNumeric(DbType type) {
        return type == INTEGER || type == LONG || type == DECIMAL;
    }
}
