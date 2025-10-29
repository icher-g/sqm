package io.sqm.render.ansi;

import io.sqm.core.LiteralExpr;
import io.sqm.render.DefaultSqlWriter;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.ParameterizationMode;
import io.sqm.render.spi.PlaceholderPreference;
import io.sqm.render.spi.Renderer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.lit;

class LiteralExprRendererTest extends BaseValuesRendererTest {

    private final Renderer<LiteralExpr> renderer = new LiteralExprRenderer();

    @Test
    void inline_single_literal_string() {
        var ctx = new TestRenderContext(ansiDialect, PlaceholderPreference.Positional, ParameterizationMode.Inline);
        SqlWriter w = new DefaultSqlWriter(ctx);

        renderer.render(lit("Igor"), ctx, w);

        assertSqlAndParams(w, ctx.params().snapshot(), "'Igor'", List.of());
    }

    @Test
    void param_positional_single() {
        var ctx = new TestRenderContext(ansiDialect, PlaceholderPreference.Positional, ParameterizationMode.Bind);
        SqlWriter w = new DefaultSqlWriter(ctx);

        // Typical renderer implementation would call bind(value, ctx, w) internally
        renderer.render(lit("Igor"), ctx, w);

        // expecting single '?'
        assertSqlAndParams(w, ctx.params().snapshot(), "?", List.of("Igor"));
    }
}
