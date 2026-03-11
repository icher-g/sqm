package io.sqm.render.mysql;

import io.sqm.dsl.Dsl;
import io.sqm.render.mysql.spi.MySqlDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MySqlFunctionExprRendererTest {

    @Test
    void rendersJsonAndStringFunctions() {
        var jsonExtract = Dsl.func("JSON_EXTRACT", Dsl.arg(Dsl.col("payload")), Dsl.arg(Dsl.lit("$.user.id")));
        var concatWs = Dsl.func("CONCAT_WS", Dsl.arg(Dsl.lit("-")), Dsl.arg(Dsl.col("first_name")), Dsl.arg(Dsl.col("last_name")));
        var ctx = RenderContext.of(new MySqlDialect());

        assertEquals("JSON_EXTRACT(payload, '$.user.id')", ctx.render(jsonExtract).sql());
        assertEquals("CONCAT_WS('-', first_name, last_name)", ctx.render(concatWs).sql());
    }

    @Test
    void rendersDateAddWithIntervalLiteralArgument() {
        var function = Dsl.func("DATE_ADD", Dsl.arg(Dsl.col("created_at")), Dsl.arg(Dsl.interval("1", "DAY")));

        var sql = RenderContext.of(new MySqlDialect()).render(function).sql();

        assertEquals("DATE_ADD(created_at, INTERVAL '1' DAY)", sql);
    }
}
