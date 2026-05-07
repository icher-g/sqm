package io.sqm.render.oracle.spi;

import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.render.defaults.DefaultOperators;
import io.sqm.render.defaults.DefaultValueFormatter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OracleDialectTest {
    private final OracleDialect dialect = new OracleDialect();

    @Test
    void exposesDialectComponents() {
        assertEquals("Oracle", dialect.name());
        assertInstanceOf(OracleIdentifierQuoter.class, dialect.quoter());
        assertInstanceOf(DefaultValueFormatter.class, dialect.formatter());
        assertInstanceOf(DefaultOperators.class, dialect.operators());
        assertInstanceOf(OracleBooleans.class, dialect.booleans());
        assertInstanceOf(OraclePaginationStyle.class, dialect.paginationStyle());
        assertNotNull(dialect.nullSorting());
        assertNotNull(dialect.renderers());
    }

    @Test
    void exposesVersionedOracleCapabilities() {
        var oracle11 = new OracleDialect(SqlDialectVersion.of(11, 2));
        var oracle19 = new OracleDialect(SqlDialectVersion.of(19, 0));

        assertFalse(oracle11.capabilities().supports(SqlFeature.LATERAL));
        assertTrue(oracle19.capabilities().supports(SqlFeature.LATERAL));
    }
}
