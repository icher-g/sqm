package io.sqm.validate.schema.model;

import io.sqm.catalog.model.CatalogType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Optional;

/**
 * Type inference and comparability helpers for {@link CatalogType}.
 */
public final class CatalogTypeSemantics {
    private CatalogTypeSemantics() {
    }

    /**
     * Infers catalog type from a Java literal value.
     *
     * @param value literal value.
     * @return inferred type if recognized.
     */
    public static Optional<CatalogType> fromLiteral(Object value) {
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof String || value instanceof Character) {
            return Optional.of(CatalogType.STRING);
        }
        if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
            return Optional.of(CatalogType.INTEGER);
        }
        if (value instanceof Long) {
            return Optional.of(CatalogType.LONG);
        }
        if (value instanceof Float || value instanceof Double) {
            return Optional.of(CatalogType.DECIMAL);
        }
        if (value instanceof Boolean) {
            return Optional.of(CatalogType.BOOLEAN);
        }
        if (value instanceof LocalDate) {
            return Optional.of(CatalogType.DATE);
        }
        if (value instanceof LocalTime) {
            return Optional.of(CatalogType.TIME);
        }
        if (value instanceof LocalDateTime) {
            return Optional.of(CatalogType.TIMESTAMP);
        }
        return Optional.empty();
    }

    /**
     * Maps SQL type name to catalog type.
     *
     * @param sqlTypeName SQL type token.
     * @return mapped type.
     */
    public static CatalogType fromSqlType(String sqlTypeName) {
        if (sqlTypeName == null) {
            return CatalogType.UNKNOWN;
        }
        return switch (sqlTypeName.toLowerCase(Locale.ROOT)) {
            case "smallint", "int", "integer", "int2", "int4" -> CatalogType.INTEGER;
            case "bigint", "int8" -> CatalogType.LONG;
            case "decimal", "numeric", "real", "double", "float", "float4", "float8" -> CatalogType.DECIMAL;
            case "boolean", "bool" -> CatalogType.BOOLEAN;
            case "char", "character", "varchar", "text", "string", "bpchar", "citext" -> CatalogType.STRING;
            case "uuid" -> CatalogType.UUID;
            case "json" -> CatalogType.JSON;
            case "jsonb" -> CatalogType.JSONB;
            case "bytea", "blob", "binary", "varbinary" -> CatalogType.BYTES;
            case "date" -> CatalogType.DATE;
            case "time", "timetz" -> CatalogType.TIME;
            case "timestamp", "timestamptz" -> CatalogType.TIMESTAMP;
            default -> CatalogType.UNKNOWN;
        };
    }

    /**
     * Indicates whether two types can be compared.
     *
     * @param left left type.
     * @param right right type.
     * @return true when comparison is semantically allowed.
     */
    public static boolean comparable(CatalogType left, CatalogType right) {
        if (!isKnown(left) || !isKnown(right)) {
            return true;
        }
        if (left == right) {
            return true;
        }
        return isNumeric(left) && isNumeric(right);
    }

    /**
     * Indicates whether a type is known.
     *
     * @param type type to test.
     * @return true when type is not {@link CatalogType#UNKNOWN}.
     */
    public static boolean isKnown(CatalogType type) {
        return type != CatalogType.UNKNOWN;
    }

    /**
     * Indicates whether a type is numeric.
     *
     * @param type type to test.
     * @return true for integer/long/decimal types.
     */
    public static boolean isNumeric(CatalogType type) {
        return type == CatalogType.INTEGER || type == CatalogType.LONG || type == CatalogType.DECIMAL;
    }
}
