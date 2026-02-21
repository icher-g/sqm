package io.sqm.catalog.postgresql;

import io.sqm.catalog.jdbc.SqlTypeMapper;
import io.sqm.catalog.model.CatalogType;

import java.sql.Types;
import java.util.Locale;

/**
 * PostgreSQL-aware mapper from JDBC/native type metadata to SQM semantic types.
 */
public final class PostgresSqlTypeMapper implements SqlTypeMapper {
    private static final PostgresSqlTypeMapper STANDARD = new PostgresSqlTypeMapper();

    private PostgresSqlTypeMapper() {
    }

    /**
     * Returns standard PostgreSQL SQL type mapper.
     *
     * @return PostgreSQL type mapper.
     */
    public static PostgresSqlTypeMapper standard() {
        return STANDARD;
    }

    /**
     * Maps PostgreSQL type metadata to SQM semantic types.
     *
     * @param nativeTypeName PostgreSQL type name, such as {@code int8} or {@code jsonb}.
     * @param jdbcType JDBC type code from {@link Types}.
     * @return mapped SQM type.
     */
    @Override
    public CatalogType map(String nativeTypeName, int jdbcType) {
        if (nativeTypeName != null) {
            var byName = mapByTypeName(nativeTypeName);
            if (byName != CatalogType.UNKNOWN) {
                return byName;
            }
        }
        return mapByJdbcType(jdbcType);
    }

    private static CatalogType mapByTypeName(String nativeTypeName) {
        var normalized = nativeTypeName.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "smallint", "int", "integer", "int2", "int4", "serial", "serial4" -> CatalogType.INTEGER;
            case "bigint", "int8", "bigserial", "serial8" -> CatalogType.LONG;
            case "decimal", "numeric", "real", "double precision", "float4", "float8", "money" -> CatalogType.DECIMAL;
            case "boolean", "bool" -> CatalogType.BOOLEAN;
            case "char", "character", "varchar", "text", "string", "bpchar", "citext", "name" -> CatalogType.STRING;
            case "uuid" -> CatalogType.UUID;
            case "json" -> CatalogType.JSON;
            case "jsonb" -> CatalogType.JSONB;
            case "bytea" -> CatalogType.BYTES;
            case "date" -> CatalogType.DATE;
            case "time", "time without time zone", "timetz", "time with time zone" -> CatalogType.TIME;
            case "timestamp", "timestamp without time zone", "timestamptz", "timestamp with time zone" -> CatalogType.TIMESTAMP;
            default -> CatalogType.UNKNOWN;
        };
    }

    private static CatalogType mapByJdbcType(int jdbcType) {
        return switch (jdbcType) {
            case Types.SMALLINT, Types.TINYINT, Types.INTEGER -> CatalogType.INTEGER;
            case Types.BIGINT -> CatalogType.LONG;
            case Types.FLOAT, Types.REAL, Types.DOUBLE, Types.DECIMAL, Types.NUMERIC -> CatalogType.DECIMAL;
            case Types.BIT, Types.BOOLEAN -> CatalogType.BOOLEAN;
            case Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR, Types.NCHAR, Types.NVARCHAR, Types.LONGNVARCHAR -> CatalogType.STRING;
            case Types.DATE -> CatalogType.DATE;
            case Types.TIME, Types.TIME_WITH_TIMEZONE -> CatalogType.TIME;
            case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> CatalogType.TIMESTAMP;
            case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB -> CatalogType.BYTES;
            default -> CatalogType.UNKNOWN;
        };
    }
}
