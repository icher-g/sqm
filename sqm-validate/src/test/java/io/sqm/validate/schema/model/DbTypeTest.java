package io.sqm.validate.schema.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class DbTypeTest {

    @Test
    void fromLiteral_mapsSupportedRuntimeTypes() {
        assertEquals(DbType.STRING, DbType.fromLiteral("x").orElseThrow());
        assertEquals(DbType.STRING, DbType.fromLiteral('x').orElseThrow());
        assertEquals(DbType.INTEGER, DbType.fromLiteral((short) 1).orElseThrow());
        assertEquals(DbType.INTEGER, DbType.fromLiteral((byte) 1).orElseThrow());
        assertEquals(DbType.INTEGER, DbType.fromLiteral(1).orElseThrow());
        assertEquals(DbType.LONG, DbType.fromLiteral(1L).orElseThrow());
        assertEquals(DbType.DECIMAL, DbType.fromLiteral(1.5f).orElseThrow());
        assertEquals(DbType.DECIMAL, DbType.fromLiteral(1.5d).orElseThrow());
        assertEquals(DbType.BOOLEAN, DbType.fromLiteral(true).orElseThrow());
        assertEquals(DbType.DATE, DbType.fromLiteral(LocalDate.now()).orElseThrow());
        assertEquals(DbType.TIME, DbType.fromLiteral(LocalTime.now()).orElseThrow());
        assertEquals(DbType.TIMESTAMP, DbType.fromLiteral(LocalDateTime.now()).orElseThrow());
    }

    @Test
    void fromLiteral_returnsEmptyForUnsupportedOrNullValues() {
        assertTrue(DbType.fromLiteral(null).isEmpty());
        assertTrue(DbType.fromLiteral(new Object()).isEmpty());
    }

    @Test
    void fromSqlType_mapsKnownAliasesAndUnknownFallback() {
        assertEquals(DbType.INTEGER, DbType.fromSqlType("INT4"));
        assertEquals(DbType.LONG, DbType.fromSqlType("bigint"));
        assertEquals(DbType.DECIMAL, DbType.fromSqlType("float8"));
        assertEquals(DbType.BOOLEAN, DbType.fromSqlType("bool"));
        assertEquals(DbType.STRING, DbType.fromSqlType("citext"));
        assertEquals(DbType.UUID, DbType.fromSqlType("uuid"));
        assertEquals(DbType.JSON, DbType.fromSqlType("json"));
        assertEquals(DbType.JSONB, DbType.fromSqlType("jsonb"));
        assertEquals(DbType.BYTES, DbType.fromSqlType("bytea"));
        assertEquals(DbType.DATE, DbType.fromSqlType("date"));
        assertEquals(DbType.TIME, DbType.fromSqlType("timetz"));
        assertEquals(DbType.TIMESTAMP, DbType.fromSqlType("timestamptz"));
        assertEquals(DbType.UNKNOWN, DbType.fromSqlType("custom_type"));
        assertEquals(DbType.UNKNOWN, DbType.fromSqlType(null));
    }

    @Test
    void comparable_andHelpers_followValidatorRules() {
        assertTrue(DbType.comparable(DbType.INTEGER, DbType.DECIMAL));
        assertTrue(DbType.comparable(DbType.UNKNOWN, DbType.STRING));
        assertTrue(DbType.comparable(DbType.STRING, DbType.UNKNOWN));
        assertTrue(DbType.comparable(DbType.STRING, DbType.STRING));
        assertFalse(DbType.comparable(DbType.STRING, DbType.INTEGER));

        assertTrue(DbType.isKnown(DbType.STRING));
        assertFalse(DbType.isKnown(DbType.UNKNOWN));
        assertTrue(DbType.isNumeric(DbType.INTEGER));
        assertTrue(DbType.isNumeric(DbType.LONG));
        assertTrue(DbType.isNumeric(DbType.DECIMAL));
        assertFalse(DbType.isNumeric(DbType.BOOLEAN));
    }
}
