package io.sqm.catalog.oracle;

import io.sqm.catalog.model.CatalogType;
import org.junit.jupiter.api.Test;

import java.sql.Types;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OracleSqlTypeMapperTest {
    private final OracleSqlTypeMapper mapper = OracleSqlTypeMapper.standard();

    @Test
    void map_prefersKnown_oracle_native_type_name() {
        assertEquals(CatalogType.STRING, mapper.map("varchar2", Types.OTHER));
        assertEquals(CatalogType.DECIMAL, mapper.map("number", Types.VARCHAR));
        assertEquals(CatalogType.TIMESTAMP, mapper.map("timestamp(6) with local time zone", Types.OTHER));
    }

    @Test
    void map_handles_oracle_aliases_case_and_precision() {
        assertEquals(CatalogType.STRING, mapper.map(" NVARCHAR2(100) ", Types.OTHER));
        assertEquals(CatalogType.BYTES, mapper.map("RAW(16)", Types.OTHER));
        assertEquals(CatalogType.JSON, mapper.map("JSON", Types.OTHER));
        assertEquals(CatalogType.STRING, mapper.map("ROWID", Types.OTHER));
        assertEquals(CatalogType.TIMESTAMP, mapper.map("TIMESTAMP(9) WITH TIME ZONE", Types.OTHER));
    }

    @Test
    void map_handles_oracle_integer_boolean_date_and_string_aliases() {
        assertEquals(CatalogType.INTEGER, mapper.map("BINARY_INTEGER", Types.OTHER));
        assertEquals(CatalogType.INTEGER, mapper.map("PLS_INTEGER", Types.OTHER));
        assertEquals(CatalogType.BOOLEAN, mapper.map("BOOLEAN", Types.OTHER));
        assertEquals(CatalogType.DATE, mapper.map("DATE", Types.OTHER));
        assertEquals(CatalogType.STRING, mapper.map("XMLTYPE", Types.OTHER));
        assertEquals(CatalogType.STRING, mapper.map("UROWID", Types.OTHER));
    }

    @Test
    void map_handles_oracle_decimal_and_binary_aliases() {
        assertEquals(CatalogType.DECIMAL, mapper.map("DEC", Types.OTHER));
        assertEquals(CatalogType.DECIMAL, mapper.map("BINARY_FLOAT", Types.OTHER));
        assertEquals(CatalogType.DECIMAL, mapper.map("BINARY_DOUBLE", Types.OTHER));
        assertEquals(CatalogType.BYTES, mapper.map("LONG RAW", Types.OTHER));
        assertEquals(CatalogType.BYTES, mapper.map("BFILE", Types.OTHER));
    }

    @Test
    void map_falls_back_to_jdbc_type_when_native_type_is_unknown() {
        assertEquals(CatalogType.STRING, mapper.map("custom_string_type", Types.CLOB));
        assertEquals(CatalogType.DECIMAL, mapper.map(null, Types.NUMERIC));
        assertEquals(CatalogType.BYTES, mapper.map("unknown_binary", Types.BLOB));
        assertEquals(CatalogType.LONG, mapper.map("custom_long_type", Types.BIGINT));
    }

    @Test
    void map_returns_unknown_when_neither_native_nor_jdbc_type_is_recognized() {
        assertEquals(CatalogType.UNKNOWN, mapper.map("sdo_geometry", Types.OTHER));
        assertEquals(CatalogType.UNKNOWN, mapper.map(null, Types.REF_CURSOR));
    }

    @Test
    void map_falls_back_to_remaining_jdbc_type_families() {
        assertEquals(CatalogType.INTEGER, mapper.map(null, Types.TINYINT));
        assertEquals(CatalogType.BOOLEAN, mapper.map(null, Types.BOOLEAN));
        assertEquals(CatalogType.STRING, mapper.map(null, Types.SQLXML));
        assertEquals(CatalogType.DATE, mapper.map(null, Types.DATE));
        assertEquals(CatalogType.TIME, mapper.map(null, Types.TIME_WITH_TIMEZONE));
        assertEquals(CatalogType.TIMESTAMP, mapper.map(null, Types.TIMESTAMP_WITH_TIMEZONE));
        assertEquals(CatalogType.BYTES, mapper.map(null, Types.LONGVARBINARY));
    }
}
