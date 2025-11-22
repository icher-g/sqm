package io.sqm.render.ansi;

import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.ParameterizationMode;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.RenderOptions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.col;

class BetweenPredicateRendererTest extends BaseValuesRendererTest {

    @Test
    void inline_range_literals() {
        var ctx = RenderContext.of(new AnsiDialect());

        var res = ctx.render(col("a").between(5, 15));

        assertSqlAndParams(res, "a BETWEEN 5 AND 15", List.of());
    }

    @Test
    void param_positional_range() {
        var ctx = RenderContext.of(new AnsiDialect());

        var res = ctx.render(col("a").between("A", "Z"), RenderOptions.of(ParameterizationMode.Bind));

        assertSqlAndParams(res, "a BETWEEN ? AND ?", List.of("A", "Z"));
    }
}
