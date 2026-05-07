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
}
