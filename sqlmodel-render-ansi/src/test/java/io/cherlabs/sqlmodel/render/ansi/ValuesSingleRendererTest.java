package io.cherlabs.sqlmodel.render.ansi;

import io.cherlabs.sqlmodel.core.Values;
import io.cherlabs.sqlmodel.render.DefaultSqlWriter;
import io.cherlabs.sqlmodel.render.Renderer;
import io.cherlabs.sqlmodel.render.SqlWriter;
import io.cherlabs.sqlmodel.render.spi.ParameterizationMode;
import io.cherlabs.sqlmodel.render.spi.PlaceholderPreference;
import org.junit.jupiter.api.Test;

import java.util.List;

class ValuesSingleRendererTest extends BaseValuesRendererTest {

    private final Renderer<Values.Single> renderer = new ValuesSingleRenderer();

    @Test
    void inline_single_literal_string() {
        var ctx = new TestRenderContext(ansiDialect, PlaceholderPreference.Positional, ParameterizationMode.Inline);
        SqlWriter w = new DefaultSqlWriter(ctx);

        renderer.render(Values.single("Igor"), ctx, w);

        assertSqlAndParams(w, ctx.params().snapshot(), "'Igor'", List.of());
    }

    @Test
    void param_positional_single() {
        var ctx = new TestRenderContext(ansiDialect, PlaceholderPreference.Positional, ParameterizationMode.Bind);
        SqlWriter w = new DefaultSqlWriter(ctx);

        // Typical renderer implementation would call bind(value, ctx, w) internally
        renderer.render(Values.single("Igor"), ctx, w);

        // expecting single '?'
        assertSqlAndParams(w, ctx.params().snapshot(), "?", List.of("Igor"));
    }
}
