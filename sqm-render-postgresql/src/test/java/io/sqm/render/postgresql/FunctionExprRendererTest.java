package io.sqm.render.postgresql;

import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FunctionExprRendererTest {
    private final RenderContext renderContext = RenderContext.of(new PostgresDialect());

    @Test
    void rendersAggregateInputOrderBy() {
        var function = func("ARRAY_AGG", col("o", "sales_channel"))
            .distinct()
            .orderBy(col("o", "sales_channel"));

        assertEquals(
            "ARRAY_AGG(DISTINCT o.sales_channel ORDER BY o.sales_channel)",
            renderContext.render(function).sql()
        );
    }

    @Test
    void rendersMultiArgumentAggregateInputOrderBy() {
        var function = func("STRING_AGG", col("name"), lit(","))
            .orderBy(col("name").desc());

        assertEquals("STRING_AGG(name, ',' ORDER BY name DESC)", renderContext.render(function).sql());
    }
}
