package io.sqm.render.postgresql;

import io.sqm.core.CteDef;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.SqlWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CteDefRendererTest {

    private static RenderContext ctx() {
        return RenderContext.of(new PostgresDialect());
    }

    private static String render(CteDef cte) {
        RenderContext ctx = ctx();
        SqlWriter w = new DefaultSqlWriter(ctx);
        w.append(cte);
        return w.toText(List.of()).sql();
    }

    private static String normalize(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    @Test
    @DisplayName("CTE materialized renders in PostgreSQL")
    void cte_materialized_renders() {
        var q = select(col("u", "id"))
            .from(tbl("users").as("u"));
        var cte = cte("u_cte", q).materialization(CteDef.Materialization.MATERIALIZED);
        var sql = normalize(render(cte));
        assertTrue(sql.startsWith("u_cte AS MATERIALIZED ("));
    }

    @Test
    @DisplayName("CTE not materialized renders in PostgreSQL")
    void cte_not_materialized_renders() {
        var q = select(col("u", "id"))
            .from(tbl("users").as("u"));
        var cte = cte("u_cte", q).materialization(CteDef.Materialization.NOT_MATERIALIZED);
        var sql = normalize(render(cte));
        assertTrue(sql.startsWith("u_cte AS NOT MATERIALIZED ("));
    }
}
