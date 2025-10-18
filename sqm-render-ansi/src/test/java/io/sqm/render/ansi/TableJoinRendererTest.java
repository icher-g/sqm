package io.sqm.render.ansi;

import io.sqm.core.*;
import io.sqm.dsl.Dsl;
import io.sqm.render.DefaultSqlWriter;
import io.sqm.render.ansi.join.TableJoinRenderer;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link TableJoinRenderer} using the real model builders and your BufferSqlWriter.
 * <p>
 * Notes:
 * - If you have a factory for a real ANSI RenderContext, replace TestRenderContext with it.
 * - We assert the canonical ANSI strings. If these fail, it will pinpoint whitespace issues in the renderer.
 */
class TableJoinRendererTest {

    private final Renderer<TableJoin> renderer = new TableJoinRenderer();

    /**
     * Minimal context just to satisfy BufferSqlWriter / renderer. Replace with your real ANSI context if available.
     */
    private String renderToSql(TableJoin join) {
        var ctx = RenderContext.of(new AnsiDialect());
        var w = new DefaultSqlWriter(ctx);
        renderer.render(join, ctx, w);
        return w.toText(List.of()).sql();
    }

    @Test
    @DisplayName("INNER JOIN -> INNER JOIN <table> ON <predicate>")
    void innerJoin_renders() {
        var join = Join.inner(Table.of("t2"))
            .on(eq(Dsl.col("t1", "id"), Dsl.col("t2", "id")));

        var sql = renderToSql(join);
        assertEquals("INNER JOIN t2 ON t1.id = t2.id", sql);
    }

    @Test
    @DisplayName("LEFT JOIN -> LEFT JOIN <table> ON <predicate>")
    void leftJoin_renders() {
        var join = Join.left(Table.of("orders"))
            .on(Filter.column(Column.of("customer_id").from("orders"))
                .eq(Column.of("id").from("customers")));

        var sql = renderToSql(join);
        // Expected canonical ANSI shape. If this fails with a missing space, add `.space()` after "LEFT JOIN" in the renderer.
        assertEquals("LEFT JOIN orders ON orders.customer_id = customers.id", sql);
    }

    @Test
    @DisplayName("RIGHT JOIN -> RIGHT JOIN <table> ON <predicate>")
    void rightJoin_renders() {
        var join = Join.right(Table.of("payments"))
            .on(Filter.column(Column.of("order_id").from("payments"))
                .eq(Column.of("id").from("orders")));

        var sql = renderToSql(join);
        // If it fails, likely missing `.space()` after "RIGHT JOIN".
        assertEquals("RIGHT JOIN payments ON payments.order_id = orders.id", sql);
    }

    @Test
    @DisplayName("FULL JOIN -> FULL JOIN <table> ON <predicate>")
    void fullJoin_renders() {
        var join = Join.full(Table.of("x"))
            .on(Filter.column(Column.of("id").from("x"))
                .eq(Column.of("id").from("y")));

        var sql = renderToSql(join);
        // If it fails, likely missing `.space()` after "FULL JOIN".
        assertEquals("FULL JOIN x ON x.id = y.id", sql);
    }

    @Test
    @DisplayName("CROSS JOIN -> CROSS JOIN <table> (no ON clause)")
    void crossJoin_renders() {
        var join = Join.cross(Table.of("dim_dates"));

        var sql = renderToSql(join);
        // If it fails with "CROSS  JOINdim_dates", replace with: w.append("CROSS JOIN").space().append(entity.table());
        assertEquals("CROSS JOIN dim_dates", sql);
    }
}
