package io.sqm.render.ansi;

import io.sqm.core.Node;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.concat;
import static io.sqm.dsl.Dsl.lit;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ConcatExprRendererTest {

    private final RenderContext ctx = RenderContext.of(new AnsiDialect());

    private String render(Node node) {
        return normalize(ctx.render(node).sql());
    }

    private String normalize(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    @Test
    void rendersConcatWithDoublePipeOperator() {
        var expr = concat(col("first_name"), lit(" "), col("last_name"));

        assertEquals("first_name || ' ' || last_name", render(expr));
    }
}
