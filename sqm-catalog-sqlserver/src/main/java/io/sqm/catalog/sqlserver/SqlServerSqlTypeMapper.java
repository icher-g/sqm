package io.sqm.catalog.sqlserver;

import io.sqm.catalog.jdbc.SqlTypeMapper;
import io.sqm.catalog.model.CatalogType;

import java.sql.Types;
import java.util.Locale;

/**
 * SQL Server-aware mapper from JDBC/native type metadata to SQM semantic types.
 */
public final class SqlServerSqlTypeMapper implements SqlTypeMapper {
    private static final SqlServerSqlTypeMapper STANDARD = new SqlServerSqlTypeMapper();

    private SqlServerSqlTypeMapper() {
    }

    /**
     * Returns standard SQL Server SQL type mapper.
     *
     * @return SQL Server type mapper.
     */
    public static SqlServerSqlTypeMapper standard() {
        return STANDARD;
    }

    /**
     * Maps SQL Server type metadata to SQM semantic types.
     *
     * @param nativeTypeName SQL Server type name, such as {@code bigint} or {@code datetimeoffset}.
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
            case "tinyint", "smallint", "int", "integer" -> CatalogType.INTEGER;
            case "bigint" -> CatalogType.LONG;
            case "decimal", "numeric", "money", "smallmoney", "float", "real" -> CatalogType.DECIMAL;
            case "bit" -> CatalogType.BOOLEAN;
            case "char", "nchar", "varchar", "nvarchar", "text", "ntext", "sysname", "xml" -> CatalogType.STRING;
            case "uniqueidentifier" -> CatalogType.UUID;
            case "json" -> CatalogType.JSON;
            case "binary", "varbinary", "image", "rowversion", "timestamp" -> CatalogType.BYTES;
            case "date" -> CatalogType.DATE;
            case "time" -> CatalogType.TIME;
            case "datetime", "datetime2", "smalldatetime", "datetimeoffset" -> CatalogType.TIMESTAMP;
            default -> CatalogType.UNKNOWN;
        };
    }

    private static CatalogType mapByJdbcType(int jdbcType) {
        return switch (jdbcType) {
            case Types.SMALLINT, Types.TINYINT, Types.INTEGER -> CatalogType.INTEGER;
            case Types.BIGINT -> CatalogType.LONG;
            case Types.FLOAT, Types.REAL, Types.DOUBLE, Types.DECIMAL, Types.NUMERIC -> CatalogType.DECIMAL;
            case Types.BIT, Types.BOOLEAN -> CatalogType.BOOLEAN;
            case Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR, Types.NCHAR, Types.NVARCHAR, Types.LONGNVARCHAR,
                Types.SQLXML -> CatalogType.STRING;
            case Types.DATE -> CatalogType.DATE;
            case Types.TIME, Types.TIME_WITH_TIMEZONE -> CatalogType.TIME;
            case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> CatalogType.TIMESTAMP;
            case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB -> CatalogType.BYTES;
            default -> CatalogType.UNKNOWN;
        };
    }
}
