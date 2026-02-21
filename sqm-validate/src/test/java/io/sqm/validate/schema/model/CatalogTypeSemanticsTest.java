package io.sqm.validate.schema.model;

import io.sqm.catalog.model.CatalogType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class CatalogTypeSemanticsTest {

    @Test
    void fromLiteral_mapsSupportedRuntimeTypes() {
        assertEquals(CatalogType.STRING, CatalogTypeSemantics.fromLiteral("x").orElseThrow());
        assertEquals(CatalogType.STRING, CatalogTypeSemantics.fromLiteral('x').orElseThrow());
        assertEquals(CatalogType.INTEGER, CatalogTypeSemantics.fromLiteral((short) 1).orElseThrow());
        assertEquals(CatalogType.INTEGER, CatalogTypeSemantics.fromLiteral((byte) 1).orElseThrow());
        assertEquals(CatalogType.INTEGER, CatalogTypeSemantics.fromLiteral(1).orElseThrow());
        assertEquals(CatalogType.LONG, CatalogTypeSemantics.fromLiteral(1L).orElseThrow());
        assertEquals(CatalogType.DECIMAL, CatalogTypeSemantics.fromLiteral(1.5f).orElseThrow());
        assertEquals(CatalogType.DECIMAL, CatalogTypeSemantics.fromLiteral(1.5d).orElseThrow());
        assertEquals(CatalogType.BOOLEAN, CatalogTypeSemantics.fromLiteral(true).orElseThrow());
        assertEquals(CatalogType.DATE, CatalogTypeSemantics.fromLiteral(LocalDate.now()).orElseThrow());
        assertEquals(CatalogType.TIME, CatalogTypeSemantics.fromLiteral(LocalTime.now()).orElseThrow());
        assertEquals(CatalogType.TIMESTAMP, CatalogTypeSemantics.fromLiteral(LocalDateTime.now()).orElseThrow());
    }

    @Test
    void fromLiteral_returnsEmptyForUnsupportedOrNullValues() {
        assertTrue(CatalogTypeSemantics.fromLiteral(null).isEmpty());
        assertTrue(CatalogTypeSemantics.fromLiteral(new Object()).isEmpty());
    }

    @Test
    void fromSqlType_mapsKnownAliasesAndUnknownFallback() {
        assertEquals(CatalogType.INTEGER, CatalogTypeSemantics.fromSqlType("INT4"));
        assertEquals(CatalogType.LONG, CatalogTypeSemantics.fromSqlType("bigint"));
        assertEquals(CatalogType.DECIMAL, CatalogTypeSemantics.fromSqlType("float8"));
        assertEquals(CatalogType.BOOLEAN, CatalogTypeSemantics.fromSqlType("bool"));
        assertEquals(CatalogType.STRING, CatalogTypeSemantics.fromSqlType("citext"));
        assertEquals(CatalogType.UUID, CatalogTypeSemantics.fromSqlType("uuid"));
        assertEquals(CatalogType.JSON, CatalogTypeSemantics.fromSqlType("json"));
        assertEquals(CatalogType.JSONB, CatalogTypeSemantics.fromSqlType("jsonb"));
        assertEquals(CatalogType.BYTES, CatalogTypeSemantics.fromSqlType("bytea"));
        assertEquals(CatalogType.DATE, CatalogTypeSemantics.fromSqlType("date"));
        assertEquals(CatalogType.TIME, CatalogTypeSemantics.fromSqlType("timetz"));
        assertEquals(CatalogType.TIMESTAMP, CatalogTypeSemantics.fromSqlType("timestamptz"));
        assertEquals(CatalogType.UNKNOWN, CatalogTypeSemantics.fromSqlType("custom_type"));
        assertEquals(CatalogType.UNKNOWN, CatalogTypeSemantics.fromSqlType(null));
    }

    @Test
    void comparable_andHelpers_followValidatorRules() {
        assertTrue(CatalogTypeSemantics.comparable(CatalogType.INTEGER, CatalogType.DECIMAL));
        assertTrue(CatalogTypeSemantics.comparable(CatalogType.UNKNOWN, CatalogType.STRING));
        assertTrue(CatalogTypeSemantics.comparable(CatalogType.STRING, CatalogType.UNKNOWN));
        assertTrue(CatalogTypeSemantics.comparable(CatalogType.STRING, CatalogType.STRING));
        assertFalse(CatalogTypeSemantics.comparable(CatalogType.STRING, CatalogType.INTEGER));

        assertTrue(CatalogTypeSemantics.isKnown(CatalogType.STRING));
        assertFalse(CatalogTypeSemantics.isKnown(CatalogType.UNKNOWN));
        assertTrue(CatalogTypeSemantics.isNumeric(CatalogType.INTEGER));
        assertTrue(CatalogTypeSemantics.isNumeric(CatalogType.LONG));
        assertTrue(CatalogTypeSemantics.isNumeric(CatalogType.DECIMAL));
        assertFalse(CatalogTypeSemantics.isNumeric(CatalogType.BOOLEAN));
    }
}


