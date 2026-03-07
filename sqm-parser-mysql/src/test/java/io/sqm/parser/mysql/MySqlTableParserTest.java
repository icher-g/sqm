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
    void parserRejectsIndexHintsWhenCapabilityIsMissing() {
        var parser = new MySqlTableParser();
        var ctx = ParseContext.of(new AnsiSpecs());
        var cur = Cursor.of("users USE INDEX (idx_users_name)", ctx.identifierQuoting());

        var result = parser.parse(cur, ctx);

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("index hints"));
    }
}
