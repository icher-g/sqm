package io.sqm.render.postgresql;

import io.sqm.core.Nulls;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.SqlWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.order;
import static org.junit.jupiter.api.Assertions.*;

class OrderItemRendererTest {

    private String renderToSql(OrderItemRenderer renderer, io.sqm.core.OrderItem item, RenderContext rc) {
        SqlWriter w = new DefaultSqlWriter(rc);
        renderer.render(item, rc, w);
        return w.toText(List.of()).sql();
    }

    @Test
    @DisplayName("USING operator renders in PostgreSQL")
    void using_operator_renders() {
        var rc = RenderContext.of(new PostgresDialect());
        var renderer = new OrderItemRenderer();

        var item = order(col("t", "c")).using("<").nulls(Nulls.FIRST);
        String sql = renderToSql(renderer, item, rc);

        int iExpr = sql.indexOf("t.c");
        int iUsing = sql.indexOf(" USING <");
        int iNulls = sql.indexOf(" NULLS FIRST");

        assertTrue(iExpr >= 0, "should render column");
        assertTrue(iUsing > iExpr, "USING after expr");
        assertTrue(iNulls > iUsing, "NULLS after USING");
        assertFalse(sql.contains(" ASC") || sql.contains(" DESC"));
    }

    @Test
    @DisplayName("Ordinal renders with direction and DEFAULT nulls mapping")
    void ordinal_with_direction_and_default_nulls() {
        var rc = RenderContext.of(new PostgresDialect());
        var renderer = new OrderItemRenderer();

        var item = io.sqm.core.OrderItem.of(3).desc().nulls(Nulls.DEFAULT);
        String sql = renderToSql(renderer, item, rc);

        assertTrue(sql.startsWith("3"), "should render ordinal");
        assertTrue(sql.contains(" DESC"), "should render direction");
        assertTrue(sql.contains(" NULLS FIRST"), "DEFAULT nulls should map to FIRST for DESC");
    }

    @Test
    @DisplayName("Collate renders with quoting when needed")
    void collate_renders_with_quoting() {
        var rc = RenderContext.of(new PostgresDialect());
        var renderer = new OrderItemRenderer();

        var item = order(col("t", "c")).collate("de-CH");
        String sql = renderToSql(renderer, item, rc);

        assertTrue(sql.contains(" COLLATE \"de-CH\""), "collation should be quoted");
        assertFalse(sql.contains(" USING "), "no USING expected");
    }
}
