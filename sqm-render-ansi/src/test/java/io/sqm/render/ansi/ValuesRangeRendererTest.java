package io.sqm.render.ansi;

import io.sqm.core.Values;
import io.sqm.render.DefaultSqlWriter;
import io.sqm.render.ansi.value.ValuesRangeRenderer;
import io.sqm.render.spi.Renderer;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.ParameterizationMode;
import io.sqm.render.spi.PlaceholderPreference;
import org.junit.jupiter.api.Test;

import java.util.List;

class ValuesRangeRendererTest extends BaseValuesRendererTest {

    private final Renderer<Values.Range> renderer = new ValuesRangeRenderer();

    @Test
    void inline_range_literals() {
        var ctx = new TestRenderContext(ansiDialect, PlaceholderPreference.Positional, ParameterizationMode.Inline);
        SqlWriter w = new DefaultSqlWriter(ctx);

        renderer.render(Values.range(5, 15), ctx, w);

        assertSqlAndParams(w, ctx.params().snapshot(), "BETWEEN 5 AND 15", List.of());
    }

    @Test
    void param_positional_range() {
        var ctx = new TestRenderContext(ansiDialect, PlaceholderPreference.Positional, ParameterizationMode.Bind);
        SqlWriter w = new DefaultSqlWriter(ctx);

        renderer.render(Values.range("A", "Z"), ctx, w);

        assertSqlAndParams(w, ctx.params().snapshot(), "BETWEEN ? AND ?", List.of("A", "Z"));
    }
}
