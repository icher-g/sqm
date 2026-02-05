package io.sqm.render.postgresql;

import io.sqm.core.Expression;
import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Renderer tests for expression-level COLLATE in PostgreSQL.
 */
class CollateExprRendererTest {

    @Test
    void renders_collate_expression() {
        RenderContext ctx = RenderContext.of(new PostgresDialect());
        Expression expr = col("name").collate("de-CH");

        String sql = ctx.render(expr).sql();
        assertTrue(sql.contains("COLLATE \"de-CH\""));
    }
}
