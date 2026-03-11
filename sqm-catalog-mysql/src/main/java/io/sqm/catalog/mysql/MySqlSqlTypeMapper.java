package io.sqm.catalog.mysql;

import io.sqm.catalog.jdbc.SqlTypeMapper;
import io.sqm.catalog.model.CatalogType;

import java.sql.Types;
import java.util.Locale;

/**
 * MySQL-aware mapper from JDBC/native type metadata to SQM semantic types.
 */
public final class MySqlSqlTypeMapper implements SqlTypeMapper {
    private static final MySqlSqlTypeMapper STANDARD = new MySqlSqlTypeMapper();

    private MySqlSqlTypeMapper() {
    }

    /**
     * Returns standard MySQL SQL type mapper.
     *
     * @return MySQL type mapper.
     */
    public static MySqlSqlTypeMapper standard() {
        return STANDARD;
    }

    /**
     * Maps MySQL type metadata to SQM semantic types.
     *
     * @param nativeTypeName MySQL type name, such as {@code bigint unsigned} or {@code json}.
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
        var normalized = nativeTypeName.toLowerCase(Locale.ROOT).trim();
        return switch (normalized) {
            case "tinyint", "tinyint unsigned", "smallint", "smallint unsigned",
                "mediumint", "mediumint unsigned", "int", "int unsigned",
                "integer", "integer unsigned", "year" -> CatalogType.INTEGER;
            case "bigint", "bigint unsigned", "serial" -> CatalogType.LONG;
            case "decimal", "dec", "numeric", "float", "double", "double precision", "real" -> CatalogType.DECIMAL;
            case "bit", "bool", "boolean" -> CatalogType.BOOLEAN;
            case "char", "varchar", "text", "tinytext", "mediumtext", "longtext", "enum", "set" -> CatalogType.STRING;
            case "json" -> CatalogType.JSON;
            case "binary", "varbinary", "blob", "tinyblob", "mediumblob", "longblob" -> CatalogType.BYTES;
            case "date" -> CatalogType.DATE;
            case "time" -> CatalogType.TIME;
            case "datetime", "timestamp" -> CatalogType.TIMESTAMP;
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
