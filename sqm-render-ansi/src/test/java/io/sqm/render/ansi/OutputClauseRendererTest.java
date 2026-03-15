package io.sqm.render.ansi;

import io.sqm.core.OutputClause;
import io.sqm.core.OutputInto;
import io.sqm.core.OutputItem;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OutputClauseRendererTest {

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    @Test
    void rendersOutputClauseWithoutIntoOrAlias() {
        var ctx = RenderContext.of(new io.sqm.render.ansi.spi.AnsiDialect());
        OutputClause clause = OutputClause.of(List.of(OutputItem.of(lit(1))));

        var sql = normalize(ctx.render(clause).sql());

        assertEquals("OUTPUT 1", sql);
    }

    @Test
    void rendersOutputClauseWithAlias() {
        var ctx = RenderContext.of(new io.sqm.render.ansi.spi.AnsiDialect());
        OutputClause clause = OutputClause.of(List.of(OutputItem.of(lit(1), id("user_id"))));

        var sql = normalize(ctx.render(clause).sql());

        assertEquals("OUTPUT 1 AS user_id", sql);
    }

    @Test
    void rendersOutputClauseWithIntoWithoutColumns() {
        var ctx = RenderContext.of(new io.sqm.render.ansi.spi.AnsiDialect());
        OutputInto into = OutputInto.of(tbl("dest"));
        OutputClause clause = OutputClause.of(List.of(OutputItem.of(lit(1))), into);

        var sql = normalize(ctx.render(clause).sql());

        assertEquals("OUTPUT 1 INTO dest", sql);
    }

    @Test
    void rendersOutputClauseWithIntoAndColumns() {
        var ctx = RenderContext.of(new io.sqm.render.ansi.spi.AnsiDialect());
        OutputInto into = OutputInto.of(tbl("dest"), List.of(id("col_a"), id("col_b")));
        OutputClause clause = OutputClause.of(List.of(OutputItem.of(lit(1))), into);

        var sql = normalize(ctx.render(clause).sql());

        assertEquals("OUTPUT 1 INTO dest (col_a, col_b)", sql);
    }
}
