package io.sqm.render.ansi;

import io.sqm.core.Filter;
import io.sqm.core.CompositeFilter;
import io.sqm.core.Column;
import io.sqm.render.DefaultSqlWriter;
import io.sqm.render.ansi.filter.CompositeFilterRenderer;
import io.sqm.render.spi.Renderer;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.ParameterizationMode;
import io.sqm.render.spi.PlaceholderPreference;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.core.Column.of;

class CompositeFilterRendererParamTest extends BaseValuesRendererTest {

    private final Renderer<CompositeFilter> renderer = new CompositeFilterRenderer();

    @Test
    void collects_params_in_traversal_order() {
        var ctx = new TestRenderContext(ansiDialect, PlaceholderPreference.Positional, ParameterizationMode.Bind);
        SqlWriter w = new DefaultSqlWriter(ctx);

        Column c1 = of("age").from("u");
        Column c2 = of("name").from("u");

        var f1 = Filter.column(c1).gt(20);
        var f2 = Filter.column(c2).like("Igor%");
        var cf = Filter.and(f1, f2);

        renderer.render(cf, ctx, w);

        // WHERE (<sql1>)\nAND (<sql2>)
        var expectedSql = """
                u.age > ?
                AND u.name LIKE ?
                """.trim();

        assertSqlAndParams(w, ctx.params().snapshot(), expectedSql, List.of(20, "Igor%"));
    }
}
