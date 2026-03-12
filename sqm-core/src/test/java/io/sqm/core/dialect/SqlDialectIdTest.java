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
    }

    @Test
    void rejectsNullAndBlankValues() {
        assertThrows(NullPointerException.class, () -> SqlDialectId.of(null));
        assertThrows(IllegalArgumentException.class, () -> SqlDialectId.of("   "));
        assertThrows(IllegalArgumentException.class, () -> new SqlDialectId(""));
    }
}
