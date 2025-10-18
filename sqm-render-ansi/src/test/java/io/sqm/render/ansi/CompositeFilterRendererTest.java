package io.sqm.render.ansi;

import io.sqm.core.Column;
import io.sqm.core.ColumnFilter;
import io.sqm.core.CompositeFilter;
import io.sqm.core.Filter;
import io.sqm.render.DefaultSqlWriter;
import io.sqm.render.SqlWriter;
import io.sqm.render.ansi.filter.CompositeFilterRenderer;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link CompositeFilterRenderer} without Mockito.
 * Uses real model objects (Column/Filter/CompositeFilter) and a tiny in-test
 * dialect + renderers repo to make the output deterministic.
 */
class CompositeFilterRendererTest {

    private final CompositeFilterRenderer sut = new CompositeFilterRenderer();

    // ---------- Helpers to build actual model filters ----------

    private static Column col(String tableAlias, String column) {
        return Column.of(column).from(tableAlias);
    }

    private static ColumnFilter eq(String tableAlias, String column, Object value) {
        return Filter.column(col(tableAlias, column)).eq(value);
    }

    // ---------- Tests ----------

    @Test
    void renders_two_filters_with_AND() {
        // Build real model filters
        ColumnFilter f1 = eq("t", "c", "X");
        ColumnFilter f2 = eq("t", "d", "Y");
        CompositeFilter root = Filter.and(f1, f2);

        RenderContext ctx = RenderContext.of(new AnsiDialect());
        SqlWriter w = new DefaultSqlWriter(ctx);

        sut.render(root, ctx, w);

        assertEquals("""
            t.c = 'X'
            AND t.d = 'Y'""".stripIndent(), w.toText(List.of()).sql());
    }

    @Test
    void renders_three_filters_with_OR() {
        ColumnFilter f1 = eq("a", "x", 1);
        ColumnFilter f2 = eq("b", "y", 2);
        ColumnFilter f3 = eq("c", "z", 3);
        CompositeFilter root = Filter.or(f1, f2, f3);

        RenderContext ctx = RenderContext.of(new AnsiDialect());
        SqlWriter w = new DefaultSqlWriter(ctx);

        sut.render(root, ctx, w);

        assertEquals("""
            a.x = 1
            OR b.y = 2
            OR c.z = 3""".stripIndent(), w.toText(List.of()).sql());
    }

    @Test
    void renders_composite_filter_containing_other_composite_filters() {
        Filter p = eq("t", "flag", true);
        CompositeFilter cf1 = Filter.not(p);
        ColumnFilter f1 = eq("a", "x", 1);
        ColumnFilter f2 = eq("b", "y", 2);
        ColumnFilter f3 = eq("c", "z", 3);
        CompositeFilter cf2 = Filter.or(f1, f2, f3);
        CompositeFilter root = Filter.and(cf1, cf2);

        RenderContext ctx = RenderContext.of(new AnsiDialect());
        SqlWriter w = new DefaultSqlWriter(ctx);

        sut.render(root, ctx, w);

        assertEquals("""
            NOT (t.flag = TRUE)
            AND (
              a.x = 1
              OR b.y = 2
              OR c.z = 3
            )""".stripIndent(), w.toText(List.of()).sql());
    }

    @Test
    void renders_NOT_with_single() {
        ColumnFilter only = eq("t", "is_deleted", false);
        CompositeFilter root = Filter.not(only);

        RenderContext ctx = RenderContext.of(new AnsiDialect());
        SqlWriter w = new DefaultSqlWriter(ctx);

        sut.render(root, ctx, w);

        assertEquals("NOT (t.is_deleted = FALSE)", w.toText(List.of()).sql());
    }
}
