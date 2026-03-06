package io.sqm.render.mysql;

import io.sqm.render.mysql.spi.MySqlDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.group;
import static io.sqm.dsl.Dsl.rollup;
import static io.sqm.dsl.Dsl.select;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MySqlGroupByRendererTest {

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

    private String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
