package io.sqm.catalog.oracle;

import io.sqm.catalog.jdbc.SqlTypeMapper;
import io.sqm.catalog.model.CatalogType;

import java.sql.Types;
import java.util.Locale;

/**
 * Oracle-aware mapper from JDBC/native type metadata to SQM semantic types.
 */
public final class OracleSqlTypeMapper implements SqlTypeMapper {
    private static final OracleSqlTypeMapper STANDARD = new OracleSqlTypeMapper();

    private OracleSqlTypeMapper() {
    }

    /**
     * Returns standard Oracle SQL type mapper.
     *
     * @return Oracle type mapper.
     */
    public static OracleSqlTypeMapper standard() {
        return STANDARD;
    }

    /**
     * Maps Oracle type metadata to SQM semantic types.
     *
     * @param nativeTypeName Oracle type name, such as {@code varchar2} or {@code timestamp(6) with time zone}.
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
        var normalized = normalize(nativeTypeName);
        return switch (normalized) {
            case "smallint", "int", "integer", "binary integer", "pls integer" -> CatalogType.INTEGER;
            case "number", "numeric", "decimal", "dec", "float", "real", "double precision",
                "binary float", "binary double" -> CatalogType.DECIMAL;
            case "boolean" -> CatalogType.BOOLEAN;
            case "char", "nchar", "varchar", "varchar2", "nvarchar2", "clob", "nclob",
                "long", "xmltype", "rowid", "urowid" -> CatalogType.STRING;
            case "json" -> CatalogType.JSON;
            case "raw", "long raw", "blob", "bfile" -> CatalogType.BYTES;
            case "date" -> CatalogType.DATE;
            case "timestamp", "timestamp with time zone", "timestamp with local time zone" -> CatalogType.TIMESTAMP;
            default -> CatalogType.UNKNOWN;
        };
    }

    private static String normalize(String nativeTypeName) {
        return nativeTypeName
            .toLowerCase(Locale.ROOT)
            .replaceAll("\\([^)]*\\)", "")
            .replace('_', ' ')
            .replaceAll("\\s+", " ")
            .trim();
    }

    private static CatalogType mapByJdbcType(int jdbcType) {
        return switch (jdbcType) {
            case Types.SMALLINT, Types.TINYINT, Types.INTEGER -> CatalogType.INTEGER;
            case Types.BIGINT -> CatalogType.LONG;
            case Types.FLOAT, Types.REAL, Types.DOUBLE, Types.DECIMAL, Types.NUMERIC -> CatalogType.DECIMAL;
            case Types.BIT, Types.BOOLEAN -> CatalogType.BOOLEAN;
            case Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR, Types.NCHAR, Types.NVARCHAR, Types.LONGNVARCHAR,
                Types.CLOB, Types.NCLOB, Types.SQLXML, Types.ROWID -> CatalogType.STRING;
            case Types.DATE -> CatalogType.DATE;
            case Types.TIME, Types.TIME_WITH_TIMEZONE -> CatalogType.TIME;
            case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> CatalogType.TIMESTAMP;
            case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB -> CatalogType.BYTES;
            default -> CatalogType.UNKNOWN;
        };
    }
}
