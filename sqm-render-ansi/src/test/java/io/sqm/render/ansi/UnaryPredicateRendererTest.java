package io.sqm.render.ansi;

import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.unary;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UnaryPredicateRendererTest {

    private final RenderContext ctx = RenderContext.of(new AnsiDialect());

    @Test
    void rendersUnaryPredicateAsExpression() {
        var predicate = unary(col("flag"));
        assertEquals("flag", ctx.render(predicate).sql());
    }
}
