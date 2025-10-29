package io.sqm.render.ansi;

import io.sqm.core.RowListExpr;
import io.sqm.render.DefaultSqlWriter;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.ParameterizationMode;
import io.sqm.render.spi.PlaceholderPreference;
import io.sqm.render.spi.Renderer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.row;
import static io.sqm.dsl.Dsl.rows;

class RowListExprRendererTest extends BaseValuesRendererTest {

    private final Renderer<RowListExpr> renderer = new RowListExprRenderer();

    @Test
    void inline_tuples_literals() {
        var ctx = new TestRenderContext(ansiDialect, PlaceholderPreference.Positional, ParameterizationMode.Inline);
        SqlWriter w = new DefaultSqlWriter(ctx);

        var tuples = rows(
            row(1, "a"),
            row(2, "b"),
            row(3, "c")
        );
        renderer.render(tuples, ctx, w);

        assertSqlAndParams(
            w, ctx.params().snapshot(),
            "(1, 'a'), (2, 'b'), (3, 'c')",
            List.of()
        );
    }

    @Test
    void param_positional_tuples() {
        var ctx = new TestRenderContext(ansiDialect, PlaceholderPreference.Positional, ParameterizationMode.Bind);
        SqlWriter w = new DefaultSqlWriter(ctx);

        var tuples = rows(
            row("x", 10),
            row("y", 20)
        );
        renderer.render(tuples, ctx, w);

        assertSqlAndParams(
            w, ctx.params().snapshot(),
            "(?, ?), (?, ?)",
            List.of("x", 10, "y", 20)
        );
    }
}
