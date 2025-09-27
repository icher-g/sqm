package io.cherlabs.sqlmodel.render.ansi;

import io.cherlabs.sqlmodel.core.Values;
import io.cherlabs.sqlmodel.render.DefaultSqlWriter;
import io.cherlabs.sqlmodel.render.Renderer;
import io.cherlabs.sqlmodel.render.SqlWriter;
import io.cherlabs.sqlmodel.render.spi.ParameterizationMode;
import io.cherlabs.sqlmodel.render.spi.PlaceholderPreference;
import org.junit.jupiter.api.Test;

import java.util.List;

class ValuesListRendererTest extends BaseValuesRendererTest {

    private final Renderer<Values.ListValues> renderer = new ValuesListRenderer();

    @Test
    void inline_list_literals() {
        var ctx = new TestRenderContext(ansiDialect, PlaceholderPreference.POSITIONAL, ParameterizationMode.INLINE);
        SqlWriter w = new DefaultSqlWriter(ctx);

        renderer.render(Values.list(List.of(1, 2, 3)), ctx, w);

        assertSqlAndParams(w, ctx.params().snapshot(), "(1, 2, 3)", List.of());
    }

    @Test
    void param_positional_list() {
        var ctx = new TestRenderContext(ansiDialect, PlaceholderPreference.POSITIONAL, ParameterizationMode.BIND);
        SqlWriter w = new DefaultSqlWriter(ctx);

        renderer.render(Values.list(List.of("a", "b", "c")), ctx, w);

        assertSqlAndParams(w, ctx.params().snapshot(), "(?, ?, ?)", List.of("a", "b", "c"));
    }

    @Test
    void param_ordinal_list() {
        var ctx = new TestRenderContext(ordinalDialect, PlaceholderPreference.ORDINAL, ParameterizationMode.BIND);
        SqlWriter w = new DefaultSqlWriter(ctx);

        renderer.render(Values.list(List.of(10, 20)), ctx, w);

        assertSqlAndParams(w, ctx.params().snapshot(), "($1, $2)", List.of(10, 20));
    }

    @Test
    void param_named_list() {
        var ctx = new TestRenderContext(namedDialect, PlaceholderPreference.NAMED, ParameterizationMode.BIND);
        SqlWriter w = new DefaultSqlWriter(ctx);

        renderer.render(Values.list(List.of(true, false)), ctx, w);

        assertSqlAndParams(w, ctx.params().snapshot(), "(:p1, :p2)", List.of(true, false));
    }
}
