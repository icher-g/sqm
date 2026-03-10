package io.sqm.core.mysql.dialect;

import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MySqlCapabilitiesTest {

    @Test
    void supports_baseline_mysql_features_for_supported_version() {
        var capabilities = MySqlCapabilities.of(SqlDialectVersion.of(8, 0));

        assertTrue(capabilities.supports(SqlFeature.DATE_TYPED_LITERAL));
        assertTrue(capabilities.supports(SqlFeature.TIME_TYPED_LITERAL));
        assertTrue(capabilities.supports(SqlFeature.TIMESTAMP_TYPED_LITERAL));
        assertTrue(capabilities.supports(SqlFeature.BIT_STRING_LITERAL));
        assertTrue(capabilities.supports(SqlFeature.HEX_STRING_LITERAL));
        assertTrue(capabilities.supports(SqlFeature.LOCKING_CLAUSE));
        assertTrue(capabilities.supports(SqlFeature.LOCKING_SHARE));
        assertTrue(capabilities.supports(SqlFeature.LOCKING_NOWAIT));
        assertTrue(capabilities.supports(SqlFeature.LOCKING_SKIP_LOCKED));
        assertTrue(capabilities.supports(SqlFeature.GROUPING_SETS));
        assertTrue(capabilities.supports(SqlFeature.ROLLUP));
        assertTrue(capabilities.supports(SqlFeature.CUBE));
        assertTrue(capabilities.supports(SqlFeature.NULL_SAFE_EQUALITY_PREDICATE));
        assertTrue(capabilities.supports(SqlFeature.REGEX_PREDICATE));
        assertTrue(capabilities.supports(SqlFeature.CALC_FOUND_ROWS_MODIFIER));
        assertTrue(capabilities.supports(SqlFeature.OPTIMIZER_HINT_COMMENT));
        assertTrue(capabilities.supports(SqlFeature.TABLE_INDEX_HINT));
        assertTrue(capabilities.supports(SqlFeature.INSERT_IGNORE));
        assertTrue(capabilities.supports(SqlFeature.INSERT_ON_DUPLICATE_KEY_UPDATE));
        assertTrue(capabilities.supports(SqlFeature.REPLACE_INTO));
        assertTrue(capabilities.supports(SqlFeature.UPDATE_JOIN));
        assertTrue(capabilities.supports(SqlFeature.DELETE_USING_JOIN));
    }

    @Test
    void does_not_support_postgresql_specific_features() {
        var capabilities = MySqlCapabilities.of(SqlDialectVersion.of(8, 0));

        assertFalse(capabilities.supports(SqlFeature.DISTINCT_ON));
        assertFalse(capabilities.supports(SqlFeature.ORDER_BY_USING));
        assertFalse(capabilities.supports(SqlFeature.POSTGRES_TYPECAST));
        assertFalse(capabilities.supports(SqlFeature.DOLLAR_STRING_LITERAL));
        assertFalse(capabilities.supports(SqlFeature.ARRAY_LITERAL));
    }

    @Test
    void latest_matches_mysql_8_0_feature_support() {
        var latest = MySqlCapabilities.latest();
        var expected = MySqlCapabilities.of(SqlDialectVersion.of(8, 0));

        assertEquals(expected.supports(SqlFeature.DATE_TYPED_LITERAL), latest.supports(SqlFeature.DATE_TYPED_LITERAL));
        assertEquals(expected.supports(SqlFeature.LOCKING_CLAUSE), latest.supports(SqlFeature.LOCKING_CLAUSE));
        assertEquals(expected.supports(SqlFeature.LOCKING_SHARE), latest.supports(SqlFeature.LOCKING_SHARE));
        assertEquals(expected.supports(SqlFeature.LOCKING_NOWAIT), latest.supports(SqlFeature.LOCKING_NOWAIT));
        assertEquals(expected.supports(SqlFeature.LOCKING_SKIP_LOCKED), latest.supports(SqlFeature.LOCKING_SKIP_LOCKED));
        assertEquals(expected.supports(SqlFeature.DISTINCT_ON), latest.supports(SqlFeature.DISTINCT_ON));
        assertEquals(expected.supports(SqlFeature.NULL_SAFE_EQUALITY_PREDICATE), latest.supports(SqlFeature.NULL_SAFE_EQUALITY_PREDICATE));
        assertEquals(expected.supports(SqlFeature.REGEX_PREDICATE), latest.supports(SqlFeature.REGEX_PREDICATE));
        assertEquals(expected.supports(SqlFeature.CALC_FOUND_ROWS_MODIFIER), latest.supports(SqlFeature.CALC_FOUND_ROWS_MODIFIER));
        assertEquals(expected.supports(SqlFeature.OPTIMIZER_HINT_COMMENT), latest.supports(SqlFeature.OPTIMIZER_HINT_COMMENT));
        assertEquals(expected.supports(SqlFeature.TABLE_INDEX_HINT), latest.supports(SqlFeature.TABLE_INDEX_HINT));
        assertEquals(expected.supports(SqlFeature.INSERT_IGNORE), latest.supports(SqlFeature.INSERT_IGNORE));
        assertEquals(expected.supports(SqlFeature.INSERT_ON_DUPLICATE_KEY_UPDATE), latest.supports(SqlFeature.INSERT_ON_DUPLICATE_KEY_UPDATE));
        assertEquals(expected.supports(SqlFeature.REPLACE_INTO), latest.supports(SqlFeature.REPLACE_INTO));
        assertEquals(expected.supports(SqlFeature.UPDATE_JOIN), latest.supports(SqlFeature.UPDATE_JOIN));
        assertEquals(expected.supports(SqlFeature.DELETE_USING_JOIN), latest.supports(SqlFeature.DELETE_USING_JOIN));
    }

    @Test
    void rejects_null_version() {
        assertThrows(NullPointerException.class, () -> MySqlCapabilities.of(null));
    }
}
