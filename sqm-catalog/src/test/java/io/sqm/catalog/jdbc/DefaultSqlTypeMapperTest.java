package io.sqm.catalog.jdbc;

import io.sqm.catalog.model.CatalogType;
import org.junit.jupiter.api.Test;

import java.sql.Types;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultSqlTypeMapperTest {
    private final DefaultSqlTypeMapper mapper = DefaultSqlTypeMapper.standard();

    @Test
    void map_prefersKnownNativeTypeName() {
        assertEquals(CatalogType.INTEGER, mapper.map("integer", Types.OTHER));
        assertEquals(CatalogType.DECIMAL, mapper.map("numeric", Types.OTHER));
        assertEquals(CatalogType.TIMESTAMP, mapper.map("timestamp with time zone", Types.OTHER));
    }

    @Test
    void map_fallsBackToJdbcTypeForUnknownNativeType() {
        assertEquals(CatalogType.STRING, mapper.map("custom_string", Types.VARCHAR));
        assertEquals(CatalogType.BOOLEAN, mapper.map(null, Types.BOOLEAN));
        assertEquals(CatalogType.BYTES, mapper.map("unknown_binary", Types.BLOB));
    }

    @Test
    void map_returnsUnknownWhenNotRecognized() {
        assertEquals(CatalogType.UNKNOWN, mapper.map("custom_type", Types.OTHER));
        assertEquals(CatalogType.UNKNOWN, mapper.map(null, Types.REF_CURSOR));
    }

    @Test
    void map_handles_additional_native_aliases_case_insensitively() {
        assertEquals(CatalogType.STRING, mapper.map("CHARACTER VARYING", Types.OTHER));
        assertEquals(CatalogType.DECIMAL, mapper.map("DOUBLE PRECISION", Types.OTHER));
        assertEquals(CatalogType.TIME, mapper.map("time without time zone", Types.OTHER));
        assertEquals(CatalogType.TIMESTAMP, mapper.map("TIMESTAMP WITHOUT TIME ZONE", Types.OTHER));
        assertEquals(CatalogType.BYTES, mapper.map("BYTEA", Types.OTHER));
    }

    @Test
    void map_covers_temporal_and_numeric_jdbc_fallbacks() {
        assertEquals(CatalogType.TIME, mapper.map("unknown", Types.TIME_WITH_TIMEZONE));
        assertEquals(CatalogType.TIMESTAMP, mapper.map("unknown", Types.TIMESTAMP_WITH_TIMEZONE));
        assertEquals(CatalogType.DATE, mapper.map("unknown", Types.DATE));
        assertEquals(CatalogType.INTEGER, mapper.map("unknown", Types.TINYINT));
        assertEquals(CatalogType.DECIMAL, mapper.map("unknown", Types.REAL));
    }
}
