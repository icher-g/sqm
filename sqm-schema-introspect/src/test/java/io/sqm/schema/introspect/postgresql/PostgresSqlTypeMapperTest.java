package io.sqm.schema.introspect.postgresql;

import io.sqm.validate.schema.model.DbType;
import org.junit.jupiter.api.Test;

import java.sql.Types;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PostgresSqlTypeMapperTest {
    private final PostgresSqlTypeMapper mapper = PostgresSqlTypeMapper.standard();

    @Test
    void map_prefersKnownPostgresNativeTypeName() {
        assertEquals(DbType.JSONB, mapper.map("jsonb", Types.OTHER));
        assertEquals(DbType.LONG, mapper.map("int8", Types.INTEGER));
        assertEquals(DbType.TIMESTAMP, mapper.map("timestamptz", Types.OTHER));
    }

    @Test
    void map_fallsBackToJdbcTypeWhenNativeTypeIsUnknown() {
        assertEquals(DbType.STRING, mapper.map("custom_string_type", Types.VARCHAR));
        assertEquals(DbType.DECIMAL, mapper.map(null, Types.NUMERIC));
        assertEquals(DbType.BYTES, mapper.map("unknown_binary", Types.BLOB));
    }

    @Test
    void map_returnsUnknownWhenNeitherNativeNorJdbcTypeIsRecognized() {
        assertEquals(DbType.UNKNOWN, mapper.map("my_enum_type", Types.OTHER));
        assertEquals(DbType.UNKNOWN, mapper.map(null, Types.REF_CURSOR));
    }

    @Test
    void map_handlesAliasesAndCaseInsensitiveTypeNames() {
        assertEquals(DbType.INTEGER, mapper.map("SERIAL4", Types.OTHER));
        assertEquals(DbType.BOOLEAN, mapper.map("BoOl", Types.OTHER));
        assertEquals(DbType.TIME, mapper.map("time with time zone", Types.OTHER));
        assertEquals(DbType.STRING, mapper.map("CITEXT", Types.OTHER));
    }

    @Test
    void map_coversJdbcFallbackForTemporalAndBinaryTypes() {
        assertEquals(DbType.TIME, mapper.map("unknown", Types.TIME_WITH_TIMEZONE));
        assertEquals(DbType.TIMESTAMP, mapper.map("unknown", Types.TIMESTAMP_WITH_TIMEZONE));
        assertEquals(DbType.DATE, mapper.map("unknown", Types.DATE));
        assertEquals(DbType.BYTES, mapper.map("unknown", Types.VARBINARY));
    }
}
