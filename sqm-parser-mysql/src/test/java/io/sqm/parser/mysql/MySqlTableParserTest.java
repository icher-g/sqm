package io.sqm.parser.mysql;

import io.sqm.core.Table;
import io.sqm.parser.ansi.AnsiSpecs;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.mysql.spi.MySqlSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MySqlTableParserTest {

    @Test
    void parsesUseIndexHint() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(Table.class, "users USE INDEX (idx_users_name)");

        assertTrue(result.ok());
        assertEquals(1, result.value().indexHints().size());
        assertEquals(Table.IndexHintType.USE, result.value().indexHints().getFirst().type());
        assertEquals("idx_users_name", result.value().indexHints().getFirst().indexes().getFirst().value());
    }

    @Test
    void parsesForceIndexForJoinHint() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(Table.class, "users u FORCE INDEX FOR JOIN (idx_join)");

        assertTrue(result.ok());
        var hint = result.value().indexHints().getFirst();
        assertEquals(Table.IndexHintType.FORCE, hint.type());
        assertEquals(Table.IndexHintScope.JOIN, hint.scope());
        assertEquals("u", result.value().alias().value());
    }

    @Test
    void parsesIgnoreIndexForOrderByHint() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(Table.class, "users IGNORE INDEX FOR ORDER BY (idx_order)");

        assertTrue(result.ok());
        var hint = result.value().indexHints().getFirst();
        assertEquals(Table.IndexHintType.IGNORE, hint.type());
        assertEquals(Table.IndexHintScope.ORDER_BY, hint.scope());
    }

    @Test
    void parsesGroupByKeyHintAndMultipleIndexes() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(Table.class, "users FORCE KEY FOR GROUP BY (idx_a, idx_b)");

        assertTrue(result.ok());
        var hint = result.value().indexHints().getFirst();
        assertEquals(Table.IndexHintType.FORCE, hint.type());
        assertEquals(Table.IndexHintScope.GROUP_BY, hint.scope());
        assertEquals(2, hint.indexes().size());
        assertEquals("idx_a", hint.indexes().get(0).value());
        assertEquals("idx_b", hint.indexes().get(1).value());
    }

    @Test
    void parsesAliasBeforeIndexHint() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(Table.class, "users u USE INDEX (idx_users_name)");

        assertTrue(result.ok());
        assertEquals("u", result.value().alias().value());
        assertEquals(Table.IndexHintType.USE, result.value().indexHints().getFirst().type());
    }

    @Test
    void rejectsInvalidIndexHintScope() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(Table.class, "users USE INDEX FOR WHERE (idx_users_name)");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Expected JOIN, ORDER BY or GROUP BY"));
    }

    @Test
    void parserRejectsIndexHintsWhenCapabilityIsMissing() {
        var parser = new MySqlTableParser();
        var ctx = ParseContext.of(new AnsiSpecs());
        var cur = Cursor.of("users USE INDEX (idx_users_name)", ctx.identifierQuoting());

        var result = parser.parse(cur, ctx);

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("index hints"));
    }
}