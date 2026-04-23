package io.sqm.render.postgresql;

import io.sqm.core.Node;
import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AnyAllPredicateRendererTest {
    private final RenderContext ctx = RenderContext.of(new PostgresDialect());

    @Test
    void rendersAnyWithArrayExpressionSource() {
        var predicate = col("c", "category_id").eqAny(col("ct", "path"));

        assertEquals("c.category_id = ANY (ct.path)", render(predicate));
    }

    @Test
    void rendersAnyWithStandardSubquerySource() {
        var predicate = col("c", "category_id").eqAny(
            select(col("category_id")).from(tbl("categories")).build()
        );

        var sql = render(predicate);

        assertEquals("c.category_id = ANY ( SELECT category_id FROM categories )", sql);
    }

    private String render(Node node) {
        return ctx.render(node).sql().replaceAll("\\s+", " ").trim();
    }
}
