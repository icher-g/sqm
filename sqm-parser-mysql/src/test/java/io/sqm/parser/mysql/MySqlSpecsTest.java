package io.sqm.parser.mysql;

import io.sqm.core.LimitOffset;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.parser.mysql.spi.MySqlSpecs;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MySqlSpecsTest {

    @Test
    void parsers_returnsRepository() {
        var specs = new MySqlSpecs();
        assertNotNull(specs.parsers());
    }

    @Test
    void lookups_isLazilyInitializedAndCached() {
        var specs = new MySqlSpecs();
        var first = specs.lookups();
        var second = specs.lookups();

        assertNotNull(first);
        assertSame(first, second);
    }

    @Test
    void identifierQuoting_supportsBacktickByDefault() {
        var specs = new MySqlSpecs();
        var quoting = specs.identifierQuoting();

        assertTrue(quoting.supports('`'));
        assertFalse(quoting.supports('"'));
    }

    @Test
    void identifierQuoting_supportsDoubleQuoteWhenAnsiQuotesModeEnabled() {
        var specs = new MySqlSpecs(SqlDialectVersion.of(8, 0), true);
        var quoting = specs.identifierQuoting();

        assertTrue(quoting.supports('`'));
        assertTrue(quoting.supports('"'));
    }

    @Test
    void parseContext_usesMysqlLimitOffsetParser() {
        var ctx = io.sqm.parser.spi.ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(LimitOffset.class, "LIMIT 5, 10");

        assertTrue(result.ok());
        assertEquals(LimitOffset.of(10L, 5L), result.value());
    }
}
