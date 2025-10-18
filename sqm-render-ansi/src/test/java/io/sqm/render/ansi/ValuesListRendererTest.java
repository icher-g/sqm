package io.sqm.render.ansi;

import io.sqm.core.Values;
import io.sqm.render.DefaultSqlWriter;
import io.sqm.render.ansi.value.ValuesListRenderer;
import io.sqm.render.spi.Renderer;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.ParameterizationMode;
import io.sqm.render.spi.PlaceholderPreference;
import org.junit.jupiter.api.Test;

import java.util.List;

class ValuesListRendererTest extends BaseValuesRendererTest {

    private final Renderer<Values.ListValues> renderer = new ValuesListRenderer();

    @Test
    void inline_list_literals() {
        var ctx = new TestRenderContext(ansiDialect, PlaceholderPreference.Positional, ParameterizationMode.Inline);
        SqlWriter w = new DefaultSqlWriter(ctx);

        renderer.render(Values.list(List.of(1, 2, 3)), ctx, w);

        assertSqlAndParams(w, ctx.params().snapshot(), "(1, 2, 3)", List.of());
    }

    @Test
    void param_positional_list() {
        var ctx = new TestRenderContext(ansiDialect, PlaceholderPreference.Positional, ParameterizationMode.Bind);
        SqlWriter w = new DefaultSqlWriter(ctx);

        renderer.render(Values.list(List.of("a", "b", "c")), ctx, w);

        assertSqlAndParams(w, ctx.params().snapshot(), "(?, ?, ?)", List.of("a", "b", "c"));
    }

    @Test
    void param_ordinal_list() {
        var ctx = new TestRenderContext(ordinalDialect, PlaceholderPreference.Ordinal, ParameterizationMode.Bind);
        SqlWriter w = new DefaultSqlWriter(ctx);

        renderer.render(Values.list(List.of(10, 20)), ctx, w);

        assertSqlAndParams(w, ctx.params().snapshot(), "($1, $2)", List.of(10, 20));
    }

    @Test
    void param_named_list() {
        var ctx = new TestRenderContext(namedDialect, PlaceholderPreference.Named, ParameterizationMode.Bind);
        SqlWriter w = new DefaultSqlWriter(ctx);

        renderer.render(Values.list(List.of(true, false)), ctx, w);

        assertSqlAndParams(w, ctx.params().snapshot(), "(:p1, :p2)", List.of(true, false));
    }
}
