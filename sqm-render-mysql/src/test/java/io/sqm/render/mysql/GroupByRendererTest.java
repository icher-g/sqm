package io.sqm.render.mysql;

import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.mysql.spi.MySqlDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.group;
import static io.sqm.dsl.Dsl.rollup;
import static io.sqm.dsl.Dsl.select;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GroupByRendererTest {

    private final RenderContext ctx = RenderContext.of(new MySqlDialect());

    @Test
    void rendersRollupAsWithRollup() {
        var query = select(col("department"), col("status"))
            .from(tbl("employees"))
            .groupBy(rollup(group("department"), group("status")))
            .build();

        var sql = normalize(ctx.render(query).sql());

        assertEquals("SELECT department, status FROM employees GROUP BY department, status WITH ROLLUP", sql);
    }

    @Test
    void rendersRegularGroupByUnchanged() {
        var query = select(col("department"))
            .from(tbl("employees"))
            .groupBy(group("department"))
            .build();

        var sql = normalize(ctx.render(query).sql());

        assertEquals("SELECT department FROM employees GROUP BY department", sql);
    }

    @Test
    void rejectsRollupWhenDialectDoesNotSupportIt() {
        var query = select(col("department"))
            .from(tbl("employees"))
            .groupBy(rollup(group("department")))
            .build();
        var renderer = new GroupByRenderer();
        var ansiCtx = RenderContext.of(new AnsiDialect());

        assertThrows(UnsupportedDialectFeatureException.class,
            () -> renderer.render(query.groupBy(), ansiCtx, new DefaultSqlWriter(ansiCtx)));
    }

    @Test
    void targetTypeIsGroupBy() {
        assertEquals(io.sqm.core.GroupBy.class, new GroupByRenderer().targetType());
    }

    private String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
