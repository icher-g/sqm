package io.sqm.render.ansi;

import io.sqm.core.BetweenPredicate;
import io.sqm.render.DefaultSqlWriter;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.ParameterizationMode;
import io.sqm.render.spi.PlaceholderPreference;
import io.sqm.render.spi.Renderer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.col;

class BetweenPredicateRendererTest extends BaseValuesRendererTest {

    private final Renderer<BetweenPredicate> renderer = new BetweenPredicateRenderer();

    @Test
    void inline_range_literals() {
        var ctx = new TestRenderContext(ansiDialect, PlaceholderPreference.Positional, ParameterizationMode.Inline);
        SqlWriter w = new DefaultSqlWriter(ctx);

        renderer.render(col("a").between(5, 15), ctx, w);

        assertSqlAndParams(w, ctx.params().snapshot(), "a BETWEEN 5 AND 15", List.of());
    }

    @Test
    void param_positional_range() {
        var ctx = new TestRenderContext(ansiDialect, PlaceholderPreference.Positional, ParameterizationMode.Bind);
        SqlWriter w = new DefaultSqlWriter(ctx);

        renderer.render(col("a").between("A", "Z"), ctx, w);

        assertSqlAndParams(w, ctx.params().snapshot(), "a BETWEEN ? AND ?", List.of("A", "Z"));
    }
}
