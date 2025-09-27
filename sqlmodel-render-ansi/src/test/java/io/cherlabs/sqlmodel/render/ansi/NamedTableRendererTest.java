package io.cherlabs.sqlmodel.render.ansi;

import io.cherlabs.sqlmodel.core.NamedTable;
import io.cherlabs.sqlmodel.core.Table;
import io.cherlabs.sqlmodel.render.DefaultSqlWriter;
import io.cherlabs.sqlmodel.render.SqlWriter;
import io.cherlabs.sqlmodel.render.SqlText;
import io.cherlabs.sqlmodel.render.ansi.spi.AnsiRenderContext;
import io.cherlabs.sqlmodel.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AnsiNamedTableRenderer.
 * - No mocks
 * - Uses BufferSqlWriter and your model/helper methods
 */
class NamedTableRendererTest {

    private final NamedTableRenderer renderer = new NamedTableRenderer();

    private String renderToSql(NamedTable table) {
        RenderContext ctx = new AnsiRenderContext();
        SqlWriter w = new DefaultSqlWriter(ctx);
        renderer.render(table, ctx, w);
        SqlText sql = w.toText(List.of());
        return sql.sql(); // adjust if your SqlText exposes the SQL differently
    }

    @Test
    @DisplayName("name only (no schema, no alias)")
    void nameOnly() {
        var t = Table.of("orders"); // helper should build NamedTable
        var sql = renderToSql(t);
        assertEquals("orders", sql);
    }

    @Test
    @DisplayName("schema + name (no alias)")
    void schemaAndName() {
        var t = Table.of("orders").from("sales");
        var sql = renderToSql(t);
        assertEquals("sales.orders", sql);
    }

    @Test
    @DisplayName("name + alias")
    void nameWithAlias() {
        var t = Table.of("orders").as("o");
        var sql = renderToSql(t);
        assertEquals("orders AS o", sql);
    }

    @Test
    @DisplayName("schema + name + alias")
    void schemaNameAlias() {
        var t = Table.of("orders").from("sales").as("o");
        var sql = renderToSql(t);
        assertEquals("sales.orders AS o", sql);
    }

    @Test
    @DisplayName("identifiers requiring quotes (dashes/uppercase) -> quoted by quoter")
    void quotingNeeded() {
        // Dashes/uppercase force quoting in ANSI
        var t = Table.of("Order-Items").from("Sales-2025").as("Line-Items");
        var sql = renderToSql(t);
        assertEquals("\"Sales-2025\".\"Order-Items\" AS \"Line-Items\"", sql);
    }

    @Test
    @DisplayName("blank schema is ignored; blank alias is ignored")
    void blanksIgnored() {
        // ensure blanks produce same as no schema/alias
        var noSchema = Table.of("orders").from("");
        assertEquals("orders", renderToSql(noSchema));

        var noAlias = Table.of("orders").as("   "); // blank alias
        assertEquals("orders", renderToSql(noAlias));
    }
}
