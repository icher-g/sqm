package io.sqm.render.ansi;

import io.sqm.core.Join;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link OnJoinRendererTest} using the real model builders and your BufferSqlWriter.
 */
class OnJoinRendererTest {

    /**
     * Minimal context just to satisfy BufferSqlWriter / renderer. Replace with your real ANSI context if available.
     */
    private String renderToSql(Join join) {
        var ctx = RenderContext.of(new AnsiDialect());
        return ctx.render(join).sql();
    }

    @Test
    @DisplayName("INNER JOIN -> INNER JOIN <table> ON <predicate>")
    void innerJoin_renders() {
        var join = inner(tbl("t2"))
            .on(col("t1", "id").eq(col("t2", "id")));

        var sql = renderToSql(join);
        assertEquals("INNER JOIN t2 ON t1.id = t2.id", sql);
    }

    @Test
    @DisplayName("LEFT JOIN -> LEFT JOIN <table> ON <predicate>")
    void leftJoin_renders() {
        var join = left(tbl("orders"))
            .on(col("customer_id").inTable("orders").eq(col("id").inTable("customers")));

        var sql = renderToSql(join);
        // Expected canonical ANSI shape. If this fails with a missing space, add `.space()` after "LEFT JOIN" in the renderer.
        assertEquals("LEFT JOIN orders ON orders.customer_id = customers.id", sql);
    }

    @Test
    @DisplayName("RIGHT JOIN -> RIGHT JOIN <table> ON <predicate>")
    void rightJoin_renders() {
        var join = right(tbl("payments"))
            .on(col("payments", "order_id")
                .eq(col("orders", "id")));

        var sql = renderToSql(join);
        // If it fails, likely missing `.space()` after "RIGHT JOIN".
        assertEquals("RIGHT JOIN payments ON payments.order_id = orders.id", sql);
    }

    @Test
    @DisplayName("FULL JOIN -> FULL JOIN <table> ON <predicate>")
    void fullJoin_renders() {
        var join = full(tbl("x"))
            .on(col("x", "id")
                .eq(col("y", "id")));

        var sql = renderToSql(join);
        // If it fails, likely missing `.space()` after "FULL JOIN".
        assertEquals("FULL JOIN x ON x.id = y.id", sql);
    }

    @Test
    @DisplayName("CROSS JOIN -> CROSS JOIN <table> (no ON clause)")
    void crossJoin_renders() {
        var join = cross(tbl("dim_dates"));

        var sql = renderToSql(join);
        // If it fails with "CROSS  JOINdim_dates", replace with: w.append("CROSS JOIN").space().append(entity.table());
        assertEquals("CROSS JOIN dim_dates", sql);
    }
}
