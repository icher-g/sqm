package io.sqm.parser.postgresql;

import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PostgresSpecsTest {

    @Test
    void parsers_returnsRepository() {
        var specs = new PostgresSpecs();
        assertNotNull(specs.parsers());
    }

    @Test
    void lookups_isLazilyInitializedAndCached() {
        var specs = new PostgresSpecs();
        var first = specs.lookups();
        var second = specs.lookups();

        assertNotNull(first);
        assertSame(first, second);
    }

    @Test
    void identifierQuoting_isLazilyInitializedAndCached() {
        var specs = new PostgresSpecs();
        var first = specs.identifierQuoting();
        var second = specs.identifierQuoting();

        assertNotNull(first);
        assertSame(first, second);
    }

    @Test
    void capabilities_supports_postgres_only_features() {
        var specs = new PostgresSpecs(SqlDialectVersion.of(12, 0));
        assertTrue(specs.capabilities().supports(SqlFeature.DOLLAR_STRING_LITERAL));
        assertTrue(specs.capabilities().supports(SqlFeature.ESCAPE_STRING_LITERAL));
        assertTrue(specs.capabilities().supports(SqlFeature.INTERVAL_LITERAL));
        assertTrue(specs.capabilities().supports(SqlFeature.DISTINCT_ON));
        assertTrue(specs.capabilities().supports(SqlFeature.ORDER_BY_USING));
        assertTrue(specs.capabilities().supports(SqlFeature.CTE_MATERIALIZATION));
        assertTrue(specs.capabilities().supports(SqlFeature.FUNCTION_TABLE));
        assertTrue(specs.capabilities().supports(SqlFeature.FUNCTION_TABLE_ORDINALITY));
        assertTrue(specs.capabilities().supports(SqlFeature.GROUPING_SETS));
        assertTrue(specs.capabilities().supports(SqlFeature.ROLLUP));
        assertTrue(specs.capabilities().supports(SqlFeature.CUBE));
        assertTrue(specs.capabilities().supports(SqlFeature.POSTGRES_TYPECAST));
        assertTrue(specs.capabilities().supports(SqlFeature.EXPONENTIATION_OPERATOR));
    }

    @Test
    void capabilities_respect_min_versions() {
        var specs = new PostgresSpecs(SqlDialectVersion.of(9, 0));
        assertFalse(specs.capabilities().supports(SqlFeature.CTE_MATERIALIZATION));
        assertFalse(specs.capabilities().supports(SqlFeature.FUNCTION_TABLE_ORDINALITY));
        assertFalse(specs.capabilities().supports(SqlFeature.GROUPING_SETS));
        assertFalse(specs.capabilities().supports(SqlFeature.ROLLUP));
        assertFalse(specs.capabilities().supports(SqlFeature.CUBE));
        assertFalse(specs.capabilities().supports(SqlFeature.LATERAL));
    }
}
