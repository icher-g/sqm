package io.sqm.render.ansi;

import io.sqm.core.Expression;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Renderer tests for expression-level COLLATE.
 */
class CollateExprRendererTest {

    @Test
    void renders_collate_expression() {
        RenderContext ctx = RenderContext.of(new AnsiDialect());
        Expression expr = col("name").collate("de-CH");

        assertThrows(UnsupportedDialectFeatureException.class, () -> ctx.render(expr).sql());
    }
}
