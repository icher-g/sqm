package io.cherlabs.sqm.render.ansi;

import io.cherlabs.sqm.core.Column;
import io.cherlabs.sqm.core.ColumnFilter;
import io.cherlabs.sqm.core.Filter;
import io.cherlabs.sqm.core.Values;
import io.cherlabs.sqm.render.DefaultSqlWriter;
import io.cherlabs.sqm.render.SqlWriter;
import io.cherlabs.sqm.render.ansi.spi.AnsiRenderContext;
import io.cherlabs.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ColumnFilterRenderer}.
 * <p>
 * Notes:
 * - Uses Mockito deep stubs to mock ctx.dialect().operators()/formatter()/renderers().
 * - Uses a minimal TestSqlWriter to capture output.
 * - Stubs per-test formatter outputs to keep assertions explicit.
 */
class ColumnFilterRendererTest {

    private final ColumnFilterRenderer renderer = new ColumnFilterRenderer();

    private static Column col(String table, String name) {
        return Column.of(name).from(table);
    }

    private String render(ColumnFilter cf) {
        var ctx = new AnsiRenderContext();
        SqlWriter w = new DefaultSqlWriter(ctx);
        renderer.render(cf, ctx, w);
        return w.toText(List.of()).sql();
    }

    @Test
    void eq_single() {
        ColumnFilter f = Filter.column(col("t", "c")).eq("X");
        var result = render(f);
        assertEquals("t.c = 'X'", result);
    }

    @Test
    void eq_column() {
        ColumnFilter f = Filter.column(col("t", "c")).eq(col("v", "d"));
        var result = render(f);
        assertEquals("t.c = v.d", result);
    }

    // --- Simple binary/unary operators (single value) ---

    @Test
    void ne_single() {
        ColumnFilter f = Filter.column(Column.of("col")).ne(42);
        var result = render(f);
        assertEquals("col <> 42", result);
    }

    @Test
    void lt_lte_gt_gte_like_notLike_single() {
        RenderContext ctx = new AnsiRenderContext();

        Column column = Column.of("c");
        SqlWriter w1 = new DefaultSqlWriter(ctx);
        renderer.render(Filter.column(column).lt(10), ctx, w1);
        assertEquals("c < 10", w1.toText(List.of()).sql());

        SqlWriter w2 = new DefaultSqlWriter(ctx);
        renderer.render(Filter.column(column).lte(11), ctx, w2);
        assertEquals("c <= 11", w2.toText(List.of()).sql());

        SqlWriter w3 = new DefaultSqlWriter(ctx);
        renderer.render(Filter.column(column).gt(12), ctx, w3);
        assertEquals("c > 12", w3.toText(List.of()).sql());

        SqlWriter w4 = new DefaultSqlWriter(ctx);
        renderer.render(Filter.column(column).gte(13), ctx, w4);
        assertEquals("c >= 13", w4.toText(List.of()).sql());

        SqlWriter w5 = new DefaultSqlWriter(ctx);
        renderer.render(Filter.column(column).like("%abc%"), ctx, w5);
        assertEquals("c LIKE '%abc%'", w5.toText(List.of()).sql());

        SqlWriter w6 = new DefaultSqlWriter(ctx);
        renderer.render(Filter.column(column).notLike("%abc%"), ctx, w6);
        assertEquals("c NOT LIKE '%abc%'", w6.toText(List.of()).sql());
    }

    @Test
    @DisplayName("IN with a single value downgrades to '='")
    void in_single_downgrades_to_eq() {
        var f = Filter.column(Column.of("c")).in(7);
        var result = render(f);
        assertEquals("c = 7", result);
    }

    // --- IN / NOT IN ---

    @Test
    void in_list_vals() {
        var f = Filter.column(Column.of("c")).in(1, 2, 3);
        var result = render(f);
        assertEquals("c IN (1, 2, 3)", result);
    }

//    @Test
//    void in_subquery() {
//        RenderContext ctx = new AnsiRenderContext();
//
//        // Subquery renderer that writes: SELECT 1
//        Query subq = new Query();
//        Renderer<Query> subqRenderer = (entity, c, w) -> w.append("SELECT 1");
//        when(ctx.dialect().renderers().requireFor(subq)).thenReturn(subqRenderer);
//
//        SqlWriter w = new BufferedSqlWriter(ctx);
//        renderer.render(filter(ColumnFilter.Operator.In, Column.of("c"), subquery(subq)), ctx, w);
//
//        assertEquals("c IN (SELECT 1)", result);
//    }

    @Test
    @DisplayName("NOT IN with a single value downgrades to '<>'")
    void notIn_single_downgrades_to_ne() {
        var f = Filter.column(Column.of("c")).notIn(7);
        var result = render(f);
        assertEquals("c <> 7", result);
    }

    @Test
    void notIn_list_vals() {
        var f = Filter.column(Column.of("c")).notIn("a", "b");
        var result = render(f);
        assertEquals("c NOT IN ('a', 'b')", result);
    }

//    @Test
//    void notIn_subquery() {
//        RenderContext ctx = new AnsiRenderContext();
//
//        Query subq = new Query();
//        Renderer<Query> subqRenderer = (entity, c, w) -> w.append("SELECT * FROM t");
//        when(ctx.dialect().renderers().requireFor(subq)).thenReturn(subqRenderer);
//
//        SqlWriter w = new BufferedSqlWriter(ctx);
//        renderer.render(filter(ColumnFilter.Operator.NotIn, Column.of("c"), subquery(subq)), ctx, w);
//
//        assertEquals("c NOT IN (SELECT * FROM t)", result);
//    }

    @Test
    void range_between() {
        var f = Filter.column(Column.of("c")).range(5, 10);
        var result = render(f);
        assertEquals("c BETWEEN 5 AND 10", result);
    }

    // --- Range / Nullability ---

    @Test
    void is_null_ignores_value() {
        var f = Filter.column(Column.of("c")).isNull();
        var result = render(f);
        assertEquals("c IS NULL", result);
    }

    @Test
    void is_not_null_ignores_value() {
        var f = Filter.column(Column.of("c")).isNotNull();
        var result = render(f);
        assertEquals("c IS NOT NULL", result);
    }

    @Nested
    @DisplayName("Supported operators")
    class SupportedOperators {

        @Test
        void rendersEq() {
            var left = col("orders", "customer_id");
            var right = col("customers", "id");
            var jf = Filter.column(left).eq(right);

            var text = render(jf);

            assertEquals("orders.customer_id = customers.id", text);
        }

        @Test
        void rendersNe() {
            var left = col("a", "x");
            var right = col("b", "y");
            var jf = Filter.column(left).ne(right);

            var text = render(jf);

            assertEquals("a.x <> b.y", text);
        }

        @Test
        void rendersLt() {
            var jf = Filter.column(col("t1", "c1")).lt(col("t2", "c2"));
            var text = render(jf);

            assertEquals("t1.c1 < t2.c2", text);
        }

        @Test
        void rendersLte() {
            var jf = Filter.column(col("t1", "c1")).lte(col("t2", "c2"));
            var text = render(jf);

            assertEquals("t1.c1 <= t2.c2", text);
        }

        @Test
        void rendersGt() {
            var jf = Filter.column(col("t1", "c1")).gt(col("t2", "c2"));
            var text = render(jf);

            assertEquals("t1.c1 > t2.c2", text);
        }

        @Test
        void rendersGte() {
            var jf = Filter.column(col("t1", "c1")).gte(col("t2", "c2"));
            var text = render(jf);

            assertEquals("t1.c1 >= t2.c2", text);
        }
    }

    // --- Error branches ---

    @Nested
    class ErrorCases {
        @Test
        void eq_with_list_throws() {
            RenderContext ctx = new AnsiRenderContext();
            ColumnFilter f = new ColumnFilter(Column.of("c"), ColumnFilter.Operator.Eq, Values.list(List.of(1, 2)));
            SqlWriter w = new DefaultSqlWriter(ctx);

            UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> renderer.render(f, ctx, w));
            assertTrue(ex.getMessage().contains("Eq"));
        }

        @Test
        void range_with_single_throws() {
            RenderContext ctx = new AnsiRenderContext();
            ColumnFilter f = new ColumnFilter(Column.of("c"), ColumnFilter.Operator.Range, Values.single(1));
            SqlWriter w = new DefaultSqlWriter(ctx);

            UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> renderer.render(f, ctx, w));
            assertTrue(ex.getMessage().contains("Range"));
        }
    }
}
