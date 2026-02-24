package io.sqm.render.ansi;

import io.sqm.core.TableRef;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for AnsiNamedTableRenderer.
 * - No mocks
 * - Uses BufferSqlWriter and your model/helper methods
 */
class TableRefRendererTest {

    private String renderToSql(TableRef table) {
        RenderContext ctx = RenderContext.of(new AnsiDialect());
        return ctx.render(table).sql();
    }

    @Test
    @DisplayName("name only (no schema, no alias)")
    void nameOnly() {
        var t = tbl("orders"); // helper should build NamedTable
        var sql = renderToSql(t);
        assertEquals("orders", sql);
    }

    @Test
    @DisplayName("schema + name (no alias)")
    void schemaAndName() {
        var t = tbl("orders").inSchema("sales");
        var sql = renderToSql(t);
        assertEquals("sales.orders", sql);
    }

    @Test
    @DisplayName("name + alias")
    void nameWithAlias() {
        var t = tbl("orders").as("o");
        var sql = renderToSql(t);
        assertEquals("orders AS o", sql);
    }

    @Test
    @DisplayName("schema + name + alias")
    void schemaNameAlias() {
        var t = tbl("orders").inSchema("sales").as("o");
        var sql = renderToSql(t);
        assertEquals("sales.orders AS o", sql);
    }

    @Test
    @DisplayName("identifiers requiring quotes (dashes/uppercase) -> quoted by quoter")
    void quotingNeeded() {
        // Dashes/uppercase force quoting in ANSI
        var t = tbl("Order-Items").inSchema("Sales-2025").as("Line-Items");
        var sql = renderToSql(t);
        assertEquals("\"Sales-2025\".\"Order-Items\" AS \"Line-Items\"", sql);
    }

    @Test
    @DisplayName("blank schema/alias identifiers are rejected by model")
    void blanksRejected() {
        assertThrows(IllegalArgumentException.class, () -> tbl("orders").inSchema(""));
        assertThrows(IllegalArgumentException.class, () -> tbl("orders").as("   "));
    }
}
