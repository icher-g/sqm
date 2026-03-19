package io.sqm.render.ansi;

import io.sqm.core.QueryExpr;
import io.sqm.core.ResultItem;
import io.sqm.core.SelectQuery;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultItemRendererTest {

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    @Test
    void rendersOutputItemWithoutAlias() {
        var ctx = RenderContext.of(new AnsiDialect());
        ResultItem item = ResultItem.expr(lit(1));

        var sql = normalize(ctx.render(item).sql());

        assertEquals("1", sql);
    }

    @Test
    void rendersOutputItemWithAlias() {
        var ctx = RenderContext.of(new AnsiDialect());
        ResultItem item = ResultItem.expr(lit(1)).as(id("out_id"));

        var sql = normalize(ctx.render(item).sql());

        assertEquals("1 AS out_id", sql);
    }

    @Test
    void rendersOutputItemWithQueryExpr() {
        var ctx = RenderContext.of(new AnsiDialect());
        SelectQuery query = select(lit(1)).from(tbl("t")).build();
        QueryExpr expr = QueryExpr.of(query);
        ResultItem item = ResultItem.expr(expr);

        var sql = normalize(ctx.render(item).sql());

        assertEquals("( SELECT 1 FROM t )", sql);
    }
}
