package io.sqm.catalog.sqlserver;

import io.sqm.catalog.model.CatalogType;
import org.junit.jupiter.api.Test;

import java.sql.Types;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SqlServerSqlTypeMapperTest {
    private final SqlServerSqlTypeMapper mapper = SqlServerSqlTypeMapper.standard();

    @Test
    void map_prefersKnownSqlServerNativeTypeName() {
        assertEquals(CatalogType.UUID, mapper.map("uniqueidentifier", Types.OTHER));
        assertEquals(CatalogType.LONG, mapper.map("bigint", Types.INTEGER));
        assertEquals(CatalogType.TIMESTAMP, mapper.map("datetimeoffset", Types.OTHER));
    }

    @Test
    void map_fallsBackToJdbcTypeWhenNativeTypeIsUnknown() {
        assertEquals(CatalogType.STRING, mapper.map("custom_nvarchar_type", Types.NVARCHAR));
        assertEquals(CatalogType.DECIMAL, mapper.map(null, Types.NUMERIC));
        assertEquals(CatalogType.BYTES, mapper.map("unknown_binary", Types.BLOB));
    }

    @Test
    void map_returnsUnknownWhenNeitherNativeNorJdbcTypeIsRecognized() {
        assertEquals(CatalogType.UNKNOWN, mapper.map("hierarchyid", Types.OTHER));
        assertEquals(CatalogType.UNKNOWN, mapper.map(null, Types.REF_CURSOR));
    }

    @Test
    void map_handlesAliasesAndCaseInsensitiveTypeNames() {
        assertEquals(CatalogType.INTEGER, mapper.map("INTEGER", Types.OTHER));
        assertEquals(CatalogType.BOOLEAN, mapper.map("BiT", Types.OTHER));
        assertEquals(CatalogType.STRING, mapper.map("SysName", Types.OTHER));
        assertEquals(CatalogType.BYTES, mapper.map("rowversion", Types.OTHER));
    }

    @Test
    void map_coversAdditionalSqlServerNativeFamilies() {
        assertEquals(CatalogType.DECIMAL, mapper.map("smallmoney", Types.OTHER));
        assertEquals(CatalogType.STRING, mapper.map("xml", Types.OTHER));
        assertEquals(CatalogType.TIME, mapper.map("time", Types.OTHER));
        assertEquals(CatalogType.DATE, mapper.map("date", Types.OTHER));
    }

    @Test
    void map_coversRemainingJdbcFallbackBuckets() {
        assertEquals(CatalogType.BOOLEAN, mapper.map("unknown_bit", Types.BIT));
        assertEquals(CatalogType.STRING, mapper.map("unknown_xml", Types.SQLXML));
        assertEquals(CatalogType.TIME, mapper.map("unknown_time_tz", Types.TIME_WITH_TIMEZONE));
        assertEquals(CatalogType.TIMESTAMP, mapper.map("unknown_timestamp_tz", Types.TIMESTAMP_WITH_TIMEZONE));
    }
}
