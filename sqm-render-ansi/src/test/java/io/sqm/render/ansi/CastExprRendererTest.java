package io.sqm.render.ansi;

import io.sqm.core.CastExpr;
import io.sqm.core.Node;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CastExprRendererTest {

    private final RenderContext ctx = RenderContext.of(new AnsiDialect());

    private String render(Node node) {
        return normalize(ctx.render(node).sql());
    }

    private String normalize(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    @Test
    void rendersCastExpression() {
        var cast = CastExpr.of(lit(1), type("int"));
        assertEquals("CAST(1 AS int)", render(cast));
    }
}
