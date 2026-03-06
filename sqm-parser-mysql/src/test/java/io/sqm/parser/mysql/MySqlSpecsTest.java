package io.sqm.parser.mysql;

import io.sqm.core.GroupBy;
import io.sqm.core.GroupItem;
import io.sqm.core.LimitOffset;
import io.sqm.core.Query;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.parser.mysql.spi.MySqlSpecs;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void identifierQuoting_isLazilyInitializedAndCached() {
        var specs = new MySqlSpecs();
        var first = specs.identifierQuoting();
        var second = specs.identifierQuoting();

        assertSame(first, second);
    }

    @Test
    void capabilities_and_operatorPolicy_are_lazily_cached() {
        var specs = new MySqlSpecs();

        assertSame(specs.capabilities(), specs.capabilities());
        assertSame(specs.operatorPolicy(), specs.operatorPolicy());
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
    void constructor_rejectsNullVersion() {
        assertThrows(NullPointerException.class, () -> new MySqlSpecs(null, false));
    }

    @Test
    void parsesQueryWithoutPagination_usingOptionalLimitOffsetPath() {
        var ctx = io.sqm.parser.spi.ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(Query.class, "SELECT id FROM users");

        assertTrue(result.ok());
    }

    @Test
    void parseContext_usesMysqlLimitOffsetParser() {
        var ctx = io.sqm.parser.spi.ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(LimitOffset.class, "LIMIT 5, 10");

        assertTrue(result.ok());
        assertEquals(LimitOffset.of(10L, 5L), result.value());
    }

    @Test
    void parseContext_usesMysqlGroupByParser_forWithRollup() {
        var ctx = io.sqm.parser.spi.ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(GroupBy.class, "GROUP BY dept, status WITH ROLLUP");

        assertTrue(result.ok());
        assertInstanceOf(GroupItem.Rollup.class, result.value().items().getFirst());
    }

    @Test
    void parseContext_usesMysqlRegexPredicateParser() {
        var ctx = io.sqm.parser.spi.ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(io.sqm.core.Predicate.class, "name RLIKE '^a'");

        assertTrue(result.ok());
        assertInstanceOf(io.sqm.core.RegexPredicate.class, result.value());
    }
}
