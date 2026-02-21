package io.sqm.catalog.jdbc;

import io.sqm.catalog.model.CatalogType;

import java.sql.Types;
import java.util.Locale;

/**
 * Default dialect-neutral SQL type mapper based on JDBC and common SQL type names.
 */
public final class DefaultSqlTypeMapper implements SqlTypeMapper {
    private static final DefaultSqlTypeMapper STANDARD = new DefaultSqlTypeMapper();

    private DefaultSqlTypeMapper() {
    }

    /**
     * Returns the standard default SQL type mapper.
     *
     * @return standard type mapper.
     */
    public static DefaultSqlTypeMapper standard() {
        return STANDARD;
    }

    /**
     * Maps native SQL/JDBC metadata to SQM catalog type.
     *
     * <p>Native type name is checked first. If the name is unknown, JDBC type
     * fallback is used.</p>
     *
     * @param nativeTypeName database-specific type name, may be null.
     * @param jdbcType JDBC type code from {@link Types}.
     * @return mapped catalog type.
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
            case "smallint", "int", "integer" -> CatalogType.INTEGER;
            case "bigint" -> CatalogType.LONG;
            case "decimal", "numeric", "real", "double", "double precision", "float" -> CatalogType.DECIMAL;
            case "boolean" -> CatalogType.BOOLEAN;
            case "char", "character", "varchar", "character varying", "text", "string" -> CatalogType.STRING;
            case "uuid" -> CatalogType.UUID;
            case "json" -> CatalogType.JSON;
            case "jsonb" -> CatalogType.JSONB;
            case "binary", "varbinary", "blob", "bytea" -> CatalogType.BYTES;
            case "date" -> CatalogType.DATE;
            case "time", "time with time zone", "time without time zone" -> CatalogType.TIME;
            case "timestamp", "timestamp with time zone", "timestamp without time zone" -> CatalogType.TIMESTAMP;
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
