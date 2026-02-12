package io.sqm.schema.introspect.postgresql;

import io.sqm.schema.introspect.jdbc.SqlTypeMapper;
import io.sqm.validate.schema.model.DbType;

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
    public DbType map(String nativeTypeName, int jdbcType) {
        if (nativeTypeName != null) {
            var byName = mapByTypeName(nativeTypeName);
            if (byName != DbType.UNKNOWN) {
                return byName;
            }
        }
        return mapByJdbcType(jdbcType);
    }

    private static DbType mapByTypeName(String nativeTypeName) {
        var normalized = nativeTypeName.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "smallint", "int", "integer", "int2", "int4", "serial", "serial4" -> DbType.INTEGER;
            case "bigint", "int8", "bigserial", "serial8" -> DbType.LONG;
            case "decimal", "numeric", "real", "double precision", "float4", "float8", "money" -> DbType.DECIMAL;
            case "boolean", "bool" -> DbType.BOOLEAN;
            case "char", "character", "varchar", "text", "string", "bpchar", "citext", "name" -> DbType.STRING;
            case "uuid" -> DbType.UUID;
            case "json" -> DbType.JSON;
            case "jsonb" -> DbType.JSONB;
            case "bytea" -> DbType.BYTES;
            case "date" -> DbType.DATE;
            case "time", "time without time zone", "timetz", "time with time zone" -> DbType.TIME;
            case "timestamp", "timestamp without time zone", "timestamptz", "timestamp with time zone" -> DbType.TIMESTAMP;
            default -> DbType.UNKNOWN;
        };
    }

    private static DbType mapByJdbcType(int jdbcType) {
        return switch (jdbcType) {
            case Types.SMALLINT, Types.TINYINT, Types.INTEGER -> DbType.INTEGER;
            case Types.BIGINT -> DbType.LONG;
            case Types.FLOAT, Types.REAL, Types.DOUBLE, Types.DECIMAL, Types.NUMERIC -> DbType.DECIMAL;
            case Types.BIT, Types.BOOLEAN -> DbType.BOOLEAN;
            case Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR, Types.NCHAR, Types.NVARCHAR, Types.LONGNVARCHAR -> DbType.STRING;
            case Types.DATE -> DbType.DATE;
            case Types.TIME, Types.TIME_WITH_TIMEZONE -> DbType.TIME;
            case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> DbType.TIMESTAMP;
            case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB -> DbType.BYTES;
            default -> DbType.UNKNOWN;
        };
    }
}

