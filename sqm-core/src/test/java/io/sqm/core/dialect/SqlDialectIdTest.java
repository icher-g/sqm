package io.sqm.core.dialect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlDialectIdTest {

    @Test
    void normalizesDialectValuesAndAliases() {
        assertEquals("mysql", SqlDialectId.of("  MySQL ").value());
        assertEquals("postgresql", SqlDialectId.of("postgres").value());
        assertEquals("postgresql", SqlDialectId.of("PostgreSQL").value());
        assertEquals(SqlDialectId.ANSI, SqlDialectId.of("ansi"));
        assertEquals(SqlDialectId.MYSQL, SqlDialectId.of("mysql"));
        assertEquals(SqlDialectId.POSTGRESQL, SqlDialectId.of("postgresql"));
        assertEquals(SqlDialectId.SQLSERVER, SqlDialectId.of("mssql"));
        assertEquals(SqlDialectId.SQLSERVER, SqlDialectId.of("tsql"));
        assertEquals(SqlDialectId.ORACLE, SqlDialectId.of("oracle"));
        assertEquals(SqlDialectId.ORACLE, SqlDialectId.of("ORA"));
    }

    @Test
    void rejectsNullAndBlankValues() {
        assertThrows(NullPointerException.class, () -> SqlDialectId.of(null));
        assertThrows(IllegalArgumentException.class, () -> SqlDialectId.of("   "));
        assertThrows(IllegalArgumentException.class, () -> new SqlDialectId(""));
    }
}
