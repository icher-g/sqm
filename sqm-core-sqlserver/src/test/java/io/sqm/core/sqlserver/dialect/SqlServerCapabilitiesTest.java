package io.sqm.core.sqlserver.dialect;

import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlServerCapabilitiesTest {

    @Test
    void supports_sql_server_baseline_features() {
        var capabilities = SqlServerCapabilities.of(SqlDialectVersion.of(2019, 0));

        assertTrue(capabilities.supports(SqlFeature.EXPR_COLLATE));
    }

    @Test
    void rejects_non_sql_server_specific_features_by_default() {
        var capabilities = SqlServerCapabilities.of(SqlDialectVersion.of(2019, 0));

        assertFalse(capabilities.supports(SqlFeature.DML_RETURNING));
        assertFalse(capabilities.supports(SqlFeature.INSERT_ON_CONFLICT));
        assertFalse(capabilities.supports(SqlFeature.INSERT_ON_DUPLICATE_KEY_UPDATE));
        assertFalse(capabilities.supports(SqlFeature.REPLACE_INTO));
        assertFalse(capabilities.supports(SqlFeature.UPDATE_JOIN));
        assertFalse(capabilities.supports(SqlFeature.DELETE_USING_JOIN));
    }
}
