package io.sqm.catalog.postgresql;

import io.sqm.catalog.model.CatalogType;
import org.junit.jupiter.api.Test;

import java.sql.Types;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PostgresSqlTypeMapperTest {
    private final PostgresSqlTypeMapper mapper = PostgresSqlTypeMapper.standard();

    @Test
    void map_prefersKnownPostgresNativeTypeName() {
        assertEquals(CatalogType.JSONB, mapper.map("jsonb", Types.OTHER));
        assertEquals(CatalogType.LONG, mapper.map("int8", Types.INTEGER));
        assertEquals(CatalogType.TIMESTAMP, mapper.map("timestamptz", Types.OTHER));
    }

    @Test
    void map_fallsBackToJdbcTypeWhenNativeTypeIsUnknown() {
        assertEquals(CatalogType.STRING, mapper.map("custom_string_type", Types.VARCHAR));
        assertEquals(CatalogType.DECIMAL, mapper.map(null, Types.NUMERIC));
        assertEquals(CatalogType.BYTES, mapper.map("unknown_binary", Types.BLOB));
    }

    @Test
    void map_returnsUnknownWhenNeitherNativeNorJdbcTypeIsRecognized() {
        assertEquals(CatalogType.UNKNOWN, mapper.map("my_enum_type", Types.OTHER));
        assertEquals(CatalogType.UNKNOWN, mapper.map(null, Types.REF_CURSOR));
    }

    @Test
    void map_handlesAliasesAndCaseInsensitiveTypeNames() {
        assertEquals(CatalogType.INTEGER, mapper.map("SERIAL4", Types.OTHER));
        assertEquals(CatalogType.BOOLEAN, mapper.map("BoOl", Types.OTHER));
        assertEquals(CatalogType.TIME, mapper.map("time with time zone", Types.OTHER));
        assertEquals(CatalogType.STRING, mapper.map("CITEXT", Types.OTHER));
    }

    @Test
    void map_coversJdbcFallbackForTemporalAndBinaryTypes() {
        assertEquals(CatalogType.TIME, mapper.map("unknown", Types.TIME_WITH_TIMEZONE));
        assertEquals(CatalogType.TIMESTAMP, mapper.map("unknown", Types.TIMESTAMP_WITH_TIMEZONE));
        assertEquals(CatalogType.DATE, mapper.map("unknown", Types.DATE));
        assertEquals(CatalogType.BYTES, mapper.map("unknown", Types.VARBINARY));
    }
}
