package io.sqm.render.ansi;

import io.sqm.core.RowExpr;
import io.sqm.render.DefaultSqlWriter;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.ParameterizationMode;
import io.sqm.render.spi.PlaceholderPreference;
import io.sqm.render.spi.Renderer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.row;

class RowExprRendererTest extends BaseValuesRendererTest {

    private final Renderer<RowExpr> renderer = new RowExprRenderer();

    @Test
    void inline_list_literals() {
        var ctx = new TestRenderContext(ansiDialect, PlaceholderPreference.Positional, ParameterizationMode.Inline);
        SqlWriter w = new DefaultSqlWriter(ctx);

        renderer.render(row(1, 2, 3), ctx, w);

        assertSqlAndParams(w, ctx.params().snapshot(), "1, 2, 3", List.of());
    }

    @Test
    void param_positional_list() {
        var ctx = new TestRenderContext(ansiDialect, PlaceholderPreference.Positional, ParameterizationMode.Bind);
        SqlWriter w = new DefaultSqlWriter(ctx);

        renderer.render(row("a", "b", "c"), ctx, w);

        assertSqlAndParams(w, ctx.params().snapshot(), "?, ?, ?", List.of("a", "b", "c"));
    }

    @Test
    void param_ordinal_list() {
        var ctx = new TestRenderContext(ordinalDialect, PlaceholderPreference.Ordinal, ParameterizationMode.Bind);
        SqlWriter w = new DefaultSqlWriter(ctx);

        renderer.render(row(10, 20), ctx, w);

        assertSqlAndParams(w, ctx.params().snapshot(), "$1, $2", List.of(10, 20));
    }

    @Test
    void param_named_list() {
        var ctx = new TestRenderContext(namedDialect, PlaceholderPreference.Named, ParameterizationMode.Bind);
        SqlWriter w = new DefaultSqlWriter(ctx);

        renderer.render(row(true, false), ctx, w);

        assertSqlAndParams(w, ctx.params().snapshot(), ":p1, :p2", List.of(true, false));
    }
}
