package io.sqm.render.ansi;

import io.sqm.core.AndPredicate;
import io.sqm.render.DefaultSqlWriter;
import io.sqm.render.SqlWriter;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.ParameterizationMode;
import io.sqm.render.spi.PlaceholderPreference;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.col;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AndPredicateRendererTest extends BaseValuesRendererTest {

    private final Renderer<AndPredicate> renderer = new AndPredicateRenderer();

    @Test
    void collects_params_in_traversal_order() {
        var ctx = new TestRenderContext(ansiDialect, PlaceholderPreference.Positional, ParameterizationMode.Bind);
        SqlWriter w = new DefaultSqlWriter(ctx);

        var c1 = col("age").inTable("u");
        var c2 = col("name").inTable("u");

        var f1 = c1.gt(20);
        var f2 = c2.like("Igor%");
        var cf = f1.and(f2);

        renderer.render(cf, ctx, w);

        // WHERE (<sql1>)\nAND (<sql2>)
        var expectedSql = "u.age > ? AND u.name LIKE ?".trim();

        assertSqlAndParams(w, ctx.params().snapshot(), expectedSql, List.of(20, "Igor%"));
    }

    @Test
    void renders_two_filters_with_AND() {
        // Build real model filters
        var f1 = col("t", "c").eq("X");
        var f2 = col("t", "d").eq("Y");
        var root = f1.and(f2);

        RenderContext ctx = RenderContext.of(new AnsiDialect());
        SqlWriter w = new DefaultSqlWriter(ctx);

        renderer.render(root, ctx, w);

        assertEquals("t.c = 'X' AND t.d = 'Y'", w.toText(List.of()).sql());
    }
}
