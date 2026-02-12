package io.sqm.core.postgresql.dialect;

import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostgresCapabilitiesTest {

    @Test
    void supports_postgres_only_features_for_supported_version() {
        var capabilities = PostgresCapabilities.of(SqlDialectVersion.of(12, 0));

        assertTrue(capabilities.supports(SqlFeature.DOLLAR_STRING_LITERAL));
        assertTrue(capabilities.supports(SqlFeature.ESCAPE_STRING_LITERAL));
        assertTrue(capabilities.supports(SqlFeature.INTERVAL_LITERAL));
        assertTrue(capabilities.supports(SqlFeature.DISTINCT_ON));
        assertTrue(capabilities.supports(SqlFeature.ORDER_BY_USING));
        assertTrue(capabilities.supports(SqlFeature.CTE_MATERIALIZATION));
        assertTrue(capabilities.supports(SqlFeature.FUNCTION_TABLE));
        assertTrue(capabilities.supports(SqlFeature.FUNCTION_TABLE_ORDINALITY));
        assertTrue(capabilities.supports(SqlFeature.GROUPING_SETS));
        assertTrue(capabilities.supports(SqlFeature.ROLLUP));
        assertTrue(capabilities.supports(SqlFeature.CUBE));
        assertTrue(capabilities.supports(SqlFeature.WINDOW_FRAME_GROUPS));
        assertTrue(capabilities.supports(SqlFeature.WINDOW_FRAME_EXCLUDE));
        assertTrue(capabilities.supports(SqlFeature.POSTGRES_TYPECAST));
        assertTrue(capabilities.supports(SqlFeature.EXPONENTIATION_OPERATOR));
        assertTrue(capabilities.supports(SqlFeature.EXPR_COLLATE));
    }

    @Test
    void supports_respects_min_versions() {
        var capabilities = PostgresCapabilities.of(SqlDialectVersion.of(9, 0));

        assertFalse(capabilities.supports(SqlFeature.CTE_MATERIALIZATION));
        assertFalse(capabilities.supports(SqlFeature.FUNCTION_TABLE_ORDINALITY));
        assertFalse(capabilities.supports(SqlFeature.GROUPING_SETS));
        assertFalse(capabilities.supports(SqlFeature.ROLLUP));
        assertFalse(capabilities.supports(SqlFeature.CUBE));
        assertFalse(capabilities.supports(SqlFeature.WINDOW_FRAME_GROUPS));
        assertFalse(capabilities.supports(SqlFeature.WINDOW_FRAME_EXCLUDE));
        assertFalse(capabilities.supports(SqlFeature.LATERAL));
    }
}
