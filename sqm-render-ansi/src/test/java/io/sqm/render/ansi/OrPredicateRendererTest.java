package io.sqm.render.ansi;

import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link OrPredicateRendererTest}.
 */
class OrPredicateRendererTest {

    // ---------- Tests ----------

    @Test
    void renders_three_filters_with_OR() {
        var f1 = col("a", "x").eq(1);
        var f2 = col("b", "y").eq(2);
        var f3 = col("c", "z").eq(3);
        var root = f1.or(f2).or(f3);

        RenderContext ctx = RenderContext.of(new AnsiDialect());
        var text = ctx.render(root);

        assertEquals("a.x = 1 OR b.y = 2 OR c.z = 3", text.sql());
    }
}
