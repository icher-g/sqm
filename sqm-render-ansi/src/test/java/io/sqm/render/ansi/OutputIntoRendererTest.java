package io.sqm.render.ansi;

import io.sqm.core.OutputInto;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.id;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OutputIntoRendererTest {

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    @Test
    void rendersIntoTargetWithoutColumns() {
        var ctx = RenderContext.of(new io.sqm.render.ansi.spi.AnsiDialect());
        OutputInto into = OutputInto.of(tbl("dest"));

        var sql = normalize(ctx.render(into).sql());

        assertEquals("INTO dest", sql);
    }

    @Test
    void rendersIntoTargetWithColumns() {
        var ctx = RenderContext.of(new io.sqm.render.ansi.spi.AnsiDialect());
        OutputInto into = OutputInto.of(tbl("dest"), java.util.List.of(id("col_a"), id("col_b")));

        var sql = normalize(ctx.render(into).sql());

        assertEquals("INTO dest (col_a, col_b)", sql);
    }
}
