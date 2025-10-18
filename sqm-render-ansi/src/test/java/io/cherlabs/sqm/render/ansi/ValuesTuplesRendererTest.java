package io.cherlabs.sqm.render.ansi;

import io.cherlabs.sqm.core.Values;
import io.cherlabs.sqm.render.DefaultSqlWriter;
import io.cherlabs.sqm.render.ansi.value.ValuesTuplesRenderer;
import io.cherlabs.sqm.render.spi.Renderer;
import io.cherlabs.sqm.render.SqlWriter;
import io.cherlabs.sqm.render.spi.ParameterizationMode;
import io.cherlabs.sqm.render.spi.PlaceholderPreference;
import org.junit.jupiter.api.Test;

import java.util.List;

class ValuesTuplesRendererTest extends BaseValuesRendererTest {

    private final Renderer<Values.Tuples> renderer = new ValuesTuplesRenderer();

    @Test
    void inline_tuples_literals() {
        var ctx = new TestRenderContext(ansiDialect, PlaceholderPreference.Positional, ParameterizationMode.Inline);
        SqlWriter w = new DefaultSqlWriter(ctx);

        var tuples = List.of(
                List.of(1, "a"),
                List.of(2, "b"),
                List.of(3, "c")
        );
        renderer.render(Values.tuples(tuples), ctx, w);

        assertSqlAndParams(
                w, ctx.params().snapshot(),
                "((1, 'a'), (2, 'b'), (3, 'c'))",
                List.of()
        );
    }

    @Test
    void param_positional_tuples() {
        var ctx = new TestRenderContext(ansiDialect, PlaceholderPreference.Positional, ParameterizationMode.Bind);
        SqlWriter w = new DefaultSqlWriter(ctx);

        var tuples = List.of(
                List.of("x", 10),
                List.of("y", 20)
        );
        renderer.render(Values.tuples(tuples), ctx, w);

        assertSqlAndParams(
                w, ctx.params().snapshot(),
                "((?, ?), (?, ?))",
                List.of("x", 10, "y", 20)
        );
    }
}
