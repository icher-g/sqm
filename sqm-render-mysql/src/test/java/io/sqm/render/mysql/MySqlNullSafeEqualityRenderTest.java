package io.sqm.render.mysql;

import io.sqm.render.mysql.spi.MySqlDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MySqlNullSafeEqualityRenderTest {

    @Test
    void rendersNullSafeEqualityOperator() {
        var ctx = RenderContext.of(new MySqlDialect());
        var sql = ctx.render(col("a").nullSafeEq(col("b"))).sql();

        assertEquals("a <=> b", sql);
    }
}


