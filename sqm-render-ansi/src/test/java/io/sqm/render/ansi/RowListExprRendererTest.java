package io.sqm.render.ansi;

import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.ParameterizationMode;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.RenderOptions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.row;
import static io.sqm.dsl.Dsl.rows;

class RowListExprRendererTest extends BaseValuesRendererTest {

    @Test
    void inline_tuples_literals() {
        var ctx = RenderContext.of(new AnsiDialect());

        var tuples = rows(
            row(1, "a"),
            row(2, "b"),
            row(3, "c")
        );
        var res = ctx.render(tuples);

        assertSqlAndParams(
            res,
            "(1, 'a'), (2, 'b'), (3, 'c')",
            List.of()
        );
    }

    @Test
    void param_positional_tuples() {
        var ctx = RenderContext.of(new AnsiDialect());

        var tuples = rows(
            row("x", 10),
            row("y", 20)
        );
        var res = ctx.render(tuples, RenderOptions.of(ParameterizationMode.Bind));

        assertSqlAndParams(
            res,
            "(?, ?), (?, ?)",
            List.of("x", 10, "y", 20)
        );
    }
}
