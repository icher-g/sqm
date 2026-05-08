package io.sqm.core.oracle.dialect;

import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OracleCapabilitiesTest {

    @Test
    void supports_baseline_oracle_features_for_supported_version() {
        var capabilities = OracleCapabilities.of(SqlDialectVersion.of(19, 0));

        assertTrue(capabilities.supports(SqlFeature.DATE_TYPED_LITERAL));
        assertTrue(capabilities.supports(SqlFeature.TIMESTAMP_TYPED_LITERAL));
        assertTrue(capabilities.supports(SqlFeature.INTERVAL_LITERAL));
        assertTrue(capabilities.supports(SqlFeature.MERGE_STATEMENT));
        assertTrue(capabilities.supports(SqlFeature.GROUPING_SETS));
        assertTrue(capabilities.supports(SqlFeature.ROLLUP));
        assertTrue(capabilities.supports(SqlFeature.CUBE));
        assertTrue(capabilities.supports(SqlFeature.LOCKING_CLAUSE));
        assertTrue(capabilities.supports(SqlFeature.LOCKING_NOWAIT));
        assertTrue(capabilities.supports(SqlFeature.LOCKING_SKIP_LOCKED));
        assertTrue(capabilities.supports(SqlFeature.OPTIMIZER_HINT_COMMENT));
        assertTrue(capabilities.supports(SqlFeature.DML_RESULT_CLAUSE));
        assertTrue(capabilities.supports(SqlFeature.DML_RESULT_VARIABLE_TARGET));
        assertTrue(capabilities.supports(SqlFeature.LATERAL));
    }

    @Test
    void does_not_support_features_outside_oracle_baseline() {
        var capabilities = OracleCapabilities.of(SqlDialectVersion.of(19, 0));

        assertFalse(capabilities.supports(SqlFeature.TIME_TYPED_LITERAL));
        assertFalse(capabilities.supports(SqlFeature.MERGE_RESULT_CLAUSE));
        assertFalse(capabilities.supports(SqlFeature.DISTINCT_ON));
        assertFalse(capabilities.supports(SqlFeature.INSERT_ON_CONFLICT));
        assertFalse(capabilities.supports(SqlFeature.INSERT_ON_DUPLICATE_KEY_UPDATE));
        assertFalse(capabilities.supports(SqlFeature.REPLACE_INTO));
        assertFalse(capabilities.supports(SqlFeature.UPDATE_JOIN));
        assertFalse(capabilities.supports(SqlFeature.DELETE_USING_JOIN));
        assertFalse(capabilities.supports(SqlFeature.TABLE_LOCK_HINT));
        assertFalse(capabilities.supports(SqlFeature.TABLE_INDEX_HINT));
        assertFalse(capabilities.supports(SqlFeature.ARRAY_LITERAL));
    }

    @Test
    void supports_lateral_starting_with_oracle_12_1() {
        assertFalse(OracleCapabilities.of(SqlDialectVersion.of(11, 2)).supports(SqlFeature.LATERAL));
        assertTrue(OracleCapabilities.of(SqlDialectVersion.of(12, 1)).supports(SqlFeature.LATERAL));
        assertTrue(OracleCapabilities.of(SqlDialectVersion.of(19, 0)).supports(SqlFeature.LATERAL));
    }

    @Test
    void latest_matches_oracle_19_feature_support() {
        var latest = OracleCapabilities.latest();
        var expected = OracleCapabilities.of(SqlDialectVersion.of(19, 0));

        assertEquals(expected.supports(SqlFeature.DATE_TYPED_LITERAL), latest.supports(SqlFeature.DATE_TYPED_LITERAL));
        assertEquals(expected.supports(SqlFeature.TIMESTAMP_TYPED_LITERAL), latest.supports(SqlFeature.TIMESTAMP_TYPED_LITERAL));
        assertEquals(expected.supports(SqlFeature.MERGE_STATEMENT), latest.supports(SqlFeature.MERGE_STATEMENT));
        assertEquals(expected.supports(SqlFeature.LATERAL), latest.supports(SqlFeature.LATERAL));
        assertEquals(expected.supports(SqlFeature.DML_RESULT_CLAUSE), latest.supports(SqlFeature.DML_RESULT_CLAUSE));
    }

    @Test
    void rejects_null_version() {
        assertThrows(NullPointerException.class, () -> OracleCapabilities.of(null));
    }
}
