package io.sqm.render.sqlserver;

import io.sqm.core.ResultClause;
import io.sqm.core.ResultInto;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.sqlserver.spi.SqlServerDialect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultClauseRendererTest {

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    @Test
    void rendersOutputClauseWithoutIntoOrAlias() {
        var ctx = RenderContext.of(new SqlServerDialect());
        ResultClause clause = result(lit(1));

        var sql = normalize(ctx.render(clause).sql());

        assertEquals("OUTPUT 1", sql);
    }

    @Test
    void rendersOutputClauseWithAlias() {
        var ctx = RenderContext.of(new SqlServerDialect());
        ResultClause clause = result(lit(1).as(id("user_id")));

        var sql = normalize(ctx.render(clause).sql());

        assertEquals("OUTPUT 1 AS user_id", sql);
    }

    @Test
    void rendersOutputClauseWithInsertedStar() {
        var ctx = RenderContext.of(new SqlServerDialect());
        ResultClause clause = result(insertedAll());

        var sql = normalize(ctx.render(clause).sql());

        assertEquals("OUTPUT inserted.*", sql);
    }

    @Test
    void rendersOutputClauseWithIntoWithoutColumns() {
        var ctx = RenderContext.of(new SqlServerDialect());
        ResultInto into = ResultInto.of(tbl("dest"));
        ResultClause clause = result(into, lit(1));

        var sql = normalize(ctx.render(clause).sql());

        assertEquals("OUTPUT 1 INTO dest", sql);
    }

    @Test
    void rendersOutputClauseWithIntoAndColumns() {
        var ctx = RenderContext.of(new SqlServerDialect());
        ResultInto into = ResultInto.of(tbl("dest"), List.of(id("col_a"), id("col_b")));
        ResultClause clause = result(into, lit(1));

        var sql = normalize(ctx.render(clause).sql());

        assertEquals("OUTPUT 1 INTO dest (col_a, col_b)", sql);
    }
}
