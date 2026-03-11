package io.sqm.catalog.mysql;

import io.sqm.catalog.model.CatalogType;
import org.junit.jupiter.api.Test;

import java.sql.Types;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MySqlSqlTypeMapperTest {
    private final MySqlSqlTypeMapper mapper = MySqlSqlTypeMapper.standard();

    @Test
    void map_prefersKnown_mysql_native_type_name() {
        assertEquals(CatalogType.JSON, mapper.map("json", Types.OTHER));
        assertEquals(CatalogType.LONG, mapper.map("bigint unsigned", Types.INTEGER));
        assertEquals(CatalogType.TIMESTAMP, mapper.map("datetime", Types.OTHER));
    }

    @Test
    void map_falls_back_to_jdbc_type_when_native_type_is_unknown() {
        assertEquals(CatalogType.STRING, mapper.map("custom_string_type", Types.VARCHAR));
        assertEquals(CatalogType.DECIMAL, mapper.map(null, Types.NUMERIC));
        assertEquals(CatalogType.BYTES, mapper.map("unknown_binary", Types.BLOB));
    }

    @Test
    void map_returns_unknown_when_neither_native_nor_jdbc_type_is_recognized() {
        assertEquals(CatalogType.UNKNOWN, mapper.map("geometrycollection", Types.OTHER));
        assertEquals(CatalogType.UNKNOWN, mapper.map(null, Types.REF_CURSOR));
    }

    @Test
    void map_handles_aliases_unsigned_and_case_insensitive_type_names() {
        assertEquals(CatalogType.INTEGER, mapper.map("YEAR", Types.OTHER));
        assertEquals(CatalogType.BOOLEAN, mapper.map("BoOl", Types.OTHER));
        assertEquals(CatalogType.STRING, mapper.map("MEDIUMTEXT", Types.OTHER));
        assertEquals(CatalogType.BYTES, mapper.map("TinyBlob", Types.OTHER));
    }

    @Test
    void map_covers_additional_mysql_type_aliases() {
        assertEquals(CatalogType.INTEGER, mapper.map("mediumint unsigned", Types.OTHER));
        assertEquals(CatalogType.LONG, mapper.map("serial", Types.OTHER));
        assertEquals(CatalogType.DECIMAL, mapper.map("double precision", Types.OTHER));
        assertEquals(CatalogType.STRING, mapper.map("enum", Types.OTHER));
        assertEquals(CatalogType.TIME, mapper.map("time", Types.OTHER));
    }

    @Test
    void map_trims_native_type_names_before_matching() {
        assertEquals(CatalogType.INTEGER, mapper.map("  integer unsigned  ", Types.OTHER));
        assertEquals(CatalogType.STRING, mapper.map("  varchar  ", Types.OTHER));
        assertEquals(CatalogType.TIMESTAMP, mapper.map("  timestamp  ", Types.OTHER));
    }

    @Test
    void map_covers_remaining_jdbc_fallback_buckets() {
        assertEquals(CatalogType.INTEGER, mapper.map("unknown_smallint", Types.SMALLINT));
        assertEquals(CatalogType.DECIMAL, mapper.map("unknown_float", Types.FLOAT));
        assertEquals(CatalogType.BOOLEAN, mapper.map("unknown_bool", Types.BIT));
        assertEquals(CatalogType.STRING, mapper.map("unknown_nvarchar", Types.NVARCHAR));
        assertEquals(CatalogType.DATE, mapper.map("unknown_date", Types.DATE));
        assertEquals(CatalogType.TIME, mapper.map("unknown_time_tz", Types.TIME_WITH_TIMEZONE));
        assertEquals(CatalogType.TIMESTAMP, mapper.map("unknown_timestamp_tz", Types.TIMESTAMP_WITH_TIMEZONE));
        assertEquals(CatalogType.BYTES, mapper.map("unknown_varbinary", Types.VARBINARY));
    }
}
