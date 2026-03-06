package io.sqm.core.mysql.dialect;

import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
