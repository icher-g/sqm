package io.sqm.render.ansi;

import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.ParameterizationMode;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.RenderOptions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.lit;

class LiteralExprRendererTest extends BaseValuesRendererTest {

    @Test
    void inline_single_literal_string() {
        var ctx = RenderContext.of(new AnsiDialect());

        var res = ctx.render(lit("Igor"));

        assertSqlAndParams(res, "'Igor'", List.of());
    }

    @Test
    void param_positional_single() {
        var ctx = RenderContext.of(new AnsiDialect());

        // Typical renderer implementation would call bind(value, ctx, w) internally
        var res = ctx.render(lit("Igor"), RenderOptions.of(ParameterizationMode.Bind));

        // expecting single '?'
        assertSqlAndParams(res, "?", List.of("Igor"));
    }
}
