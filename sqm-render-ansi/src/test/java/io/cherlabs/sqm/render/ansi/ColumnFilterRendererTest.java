package io.cherlabs.sqm.render.ansi;

import io.cherlabs.sqm.core.Column;
import io.cherlabs.sqm.core.ColumnFilter;
import io.cherlabs.sqm.core.Filter;
import io.cherlabs.sqm.core.Values;
import io.cherlabs.sqm.render.ansi.spi.AnsiDialect;
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

    private static Column col(String table, String name) {
        return Column.of(name).from(table);
    }

    private String render(ColumnFilter cf) {
        var ctx = RenderContext.of(new AnsiDialect());
        return ctx.render(cf).sql();
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
        var ctx = RenderContext.of(new AnsiDialect());

        Column column = Column.of("c");
        var w1 = ctx.render(Filter.column(column).lt(10));
        assertEquals("c < 10", w1.sql());

        var w2 = ctx.render(Filter.column(column).lte(11));
        assertEquals("c <= 11", w2.sql());

        var w3 = ctx.render(Filter.column(column).gt(12));
        assertEquals("c > 12", w3.sql());

        var w4 = ctx.render(Filter.column(column).gte(13));
        assertEquals("c >= 13", w4.sql());

        var w5 = ctx.render(Filter.column(column).like("%abc%"));
        assertEquals("c LIKE '%abc%'", w5.sql());

        var w6 = ctx.render(Filter.column(column).notLike("%abc%"));
        assertEquals("c NOT LIKE '%abc%'", w6.sql());
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
            var ctx = RenderContext.of(new AnsiDialect());
            ColumnFilter f = new ColumnFilter(Column.of("c"), ColumnFilter.Operator.Eq, Values.list(List.of(1, 2)));

            UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> ctx.render(f));
            assertTrue(ex.getMessage().contains("Eq"));
        }

        @Test
        void range_with_single_throws() {
            var ctx = RenderContext.of(new AnsiDialect());
            ColumnFilter f = new ColumnFilter(Column.of("c"), ColumnFilter.Operator.Range, Values.single(1));

            UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> ctx.render(f));
            assertTrue(ex.getMessage().contains("Range"));
        }
    }
}
