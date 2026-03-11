package io.sqm.parser.mysql;

import io.sqm.core.DeleteStatement;
import io.sqm.core.GroupBy;
import io.sqm.core.GroupItem;
import io.sqm.core.InsertStatement;
import io.sqm.core.LimitOffset;
import io.sqm.core.Query;
import io.sqm.core.UpdateStatement;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.mysql.spi.MySqlSpecs;
import io.sqm.parser.mysql.spi.MySqlSqlMode;
import org.junit.jupiter.api.Test;

import java.util.Set;

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
        assertFalse(specs.capabilities().supports(SqlFeature.DML_RETURNING));
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
        var specs = new MySqlSpecs(SqlDialectVersion.of(8, 0), Set.of(MySqlSqlMode.ANSI_QUOTES));
        var quoting = specs.identifierQuoting();

        assertTrue(quoting.supports('`'));
        assertTrue(quoting.supports('"'));
        assertEquals(Set.of(MySqlSqlMode.ANSI_QUOTES), specs.sqlModes());
    }

    @Test
    void constructor_rejectsNullVersion() {
        assertThrows(NullPointerException.class, () -> new MySqlSpecs(null, false));
    }

    @Test
    void constructor_rejectsNullSqlModes() {
        assertThrows(NullPointerException.class, () -> new MySqlSpecs(SqlDialectVersion.of(8, 0), null));
    }

    @Test
    void booleanAnsiQuotesConstructorMapsToSqlMode() {
        var specs = new MySqlSpecs(SqlDialectVersion.of(8, 0), true);

        assertEquals(Set.of(MySqlSqlMode.ANSI_QUOTES), specs.sqlModes());
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

    @Test
    void parseContext_usesMysqlSelectQueryParser_forSqlCalcFoundRows() {
        var ctx = io.sqm.parser.spi.ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(Query.class, "SELECT SQL_CALC_FOUND_ROWS id FROM users");

        assertTrue(result.ok());
        var select = assertInstanceOf(io.sqm.core.SelectQuery.class, result.value());
        assertEquals(io.sqm.core.SelectModifier.CALC_FOUND_ROWS, select.modifiers().getFirst());
    }

    @Test
    void parseContext_usesMysqlTableParser_forIndexHints() {
        var ctx = io.sqm.parser.spi.ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(io.sqm.core.Table.class, "users USE INDEX (idx_users_name)");

        assertTrue(result.ok());
        assertEquals(io.sqm.core.Table.IndexHintType.USE, result.value().indexHints().getFirst().type());
    }

    @Test
    void parseContext_usesMysqlInsertParser_forInsertIgnore() {
        var ctx = io.sqm.parser.spi.ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(InsertStatement.class, "INSERT IGNORE INTO users VALUES (1)");

        assertTrue(result.ok());
        assertEquals(InsertStatement.InsertMode.IGNORE, result.value().insertMode());
    }

    @Test
    void parseContext_usesMysqlUpdateParser_forJoinedUpdate() {
        var ctx = io.sqm.parser.spi.ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(UpdateStatement.class,
            "UPDATE users INNER JOIN orders ON users.id = orders.user_id SET name = 'alice'");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals(1, result.value().joins().size());
    }

    @Test
    void parseContext_usesMysqlDeleteParser_forUsingJoinDelete() {
        var ctx = io.sqm.parser.spi.ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(DeleteStatement.class,
            "DELETE FROM users USING users INNER JOIN orders ON users.id = orders.user_id");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals(1, result.value().joins().size());
    }

    @Test
    void parseContext_respectsAnsiQuotesSqlModeForQuotedIdentifiers() {
        var ctx = io.sqm.parser.spi.ParseContext.of(new MySqlSpecs(SqlDialectVersion.of(8, 0), Set.of(MySqlSqlMode.ANSI_QUOTES)));
        var result = ctx.parse(Query.class, "SELECT \"id\" FROM \"users\"");

        assertTrue(result.ok(), result.errorMessage());
        var query = assertInstanceOf(io.sqm.core.SelectQuery.class, result.value());
        assertEquals("users", query.from().matchTableRef().table(t -> t.name().value()).orElseThrow(IllegalStateException::new));
    }

    @Test
    void parseContext_usesMysqlLookupsForUnquotedIntervalLiteral() {
        var ctx = io.sqm.parser.spi.ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(io.sqm.core.Expression.class, "INTERVAL 1 DAY");

        assertTrue(result.ok(), result.errorMessage());
        var interval = assertInstanceOf(io.sqm.core.IntervalLiteralExpr.class, result.value());
        assertEquals("1", interval.value());
        assertEquals("DAY", interval.qualifier().orElseThrow());
    }

    @Test
    void parseContext_usesMysqlLookupsForSignedUnquotedIntervalLiteral() {
        var ctx = io.sqm.parser.spi.ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(io.sqm.core.Expression.class, "INTERVAL -1 DAY");

        assertTrue(result.ok(), result.errorMessage());
        var interval = assertInstanceOf(io.sqm.core.IntervalLiteralExpr.class, result.value());
        assertEquals("-1", interval.value());
        assertEquals("DAY", interval.qualifier().orElseThrow());
    }
}
