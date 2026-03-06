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
        assertTrue(capabilities.supports(SqlFeature.GROUPING_SETS));
        assertTrue(capabilities.supports(SqlFeature.ROLLUP));
        assertTrue(capabilities.supports(SqlFeature.CUBE));
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
        assertEquals(expected.supports(SqlFeature.DISTINCT_ON), latest.supports(SqlFeature.DISTINCT_ON));
    }

    @Test
    void rejects_null_version() {
        assertThrows(NullPointerException.class, () -> MySqlCapabilities.of(null));
    }
}
