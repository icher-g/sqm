package io.sqm.core.dialect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VersionedDialectCapabilitiesTest {

    @Test
    void supports_respectsMinimumVersions() {
        var caps = VersionedDialectCapabilities.builder(SqlDialectVersion.of(12))
            .supports(SqlFeature.DATE_TYPED_LITERAL)
            .supports(SqlFeature.CTE_MATERIALIZATION, SqlDialectVersion.of(12))
            .supports(SqlFeature.DISTINCT_ON, SqlDialectVersion.of(9, 6))
            .build();

        assertTrue(caps.supports(SqlFeature.DATE_TYPED_LITERAL));
        assertTrue(caps.supports(SqlFeature.CTE_MATERIALIZATION));
        assertTrue(caps.supports(SqlFeature.DISTINCT_ON));
        assertFalse(caps.supports(SqlFeature.DOLLAR_STRING_LITERAL));
    }

    @Test
    void supports_returnsFalseWhenVersionTooLow() {
        var caps = VersionedDialectCapabilities.builder(SqlDialectVersion.of(11))
            .supports(SqlFeature.CTE_MATERIALIZATION, SqlDialectVersion.of(12))
            .build();

        assertFalse(caps.supports(SqlFeature.CTE_MATERIALIZATION));
    }
}
