package io.sqm.core.dialect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlDialectVersionTest {

    @Test
    void compareTo_ordersByMajorMinorPatch() {
        assertTrue(SqlDialectVersion.of(13).isAtLeast(SqlDialectVersion.of(12, 3, 9)));
        assertTrue(SqlDialectVersion.of(12, 4).isAtLeast(SqlDialectVersion.of(12, 3, 9)));
        assertTrue(SqlDialectVersion.of(12, 3, 9).isAtLeast(SqlDialectVersion.of(12, 3, 9)));
        assertFalse(SqlDialectVersion.of(12, 3, 8).isAtLeast(SqlDialectVersion.of(12, 3, 9)));
    }

    @Test
    void minimum_isNonNegative() {
        var min = SqlDialectVersion.minimum();
        assertEquals(0, min.major());
        assertEquals(0, min.minor());
        assertEquals(0, min.patch());
    }
}
