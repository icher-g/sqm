package io.sqm.render.ansi;

import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.ParameterizationMode;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.RenderOptions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.row;

class RowExprRendererTest extends BaseValuesRendererTest {

    @Test
    void inline_list_literals() {
        var ctx = RenderContext.of(new AnsiDialect());

        var res = ctx.render(row(1, 2, 3));

        assertSqlAndParams(res, "1, 2, 3", List.of());
    }

    @Test
    void param_positional_list() {
        var ctx = RenderContext.of(new AnsiDialect());

        var res = ctx.render(row("a", "b", "c"), RenderOptions.of(ParameterizationMode.Bind));

        assertSqlAndParams(res, "?, ?, ?", List.of("a", "b", "c"));
    }

    @Test
    void param_ordinal_list() {
        var ctx = RenderContext.of(new AnsiDialect());

        var res = ctx.render(row(10, 20), RenderOptions.of(ParameterizationMode.Bind));

        assertSqlAndParams(res, "?, ?", List.of(10, 20));
    }

    @Test
    void param_named_list() {
        var ctx = RenderContext.of(new AnsiDialect());

        var res = ctx.render(row(true, false), RenderOptions.of(ParameterizationMode.Bind));

        assertSqlAndParams(res, "?, ?", List.of(true, false));
    }
}
