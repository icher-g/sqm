package io.sqm.render.ansi;

import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CompositePredicatesRendererTest {

    @Test
    void renders_composite_filter_containing_other_composite_filters() {
        var p = col("t", "flag").eq(true);
        var cf1 = p.not();
        var f1 = col("a", "x").eq(1);
        var f2 = col("b", "y").eq(2);
        var f3 = col("c", "z").eq(3);
        var cf2 = f1.or(f2).or(f3);
        var root = cf1.and(cf2);

        RenderContext ctx = RenderContext.of(new AnsiDialect());
        var text = ctx.render(root);

        assertEquals("NOT (t.flag = TRUE) AND (a.x = 1 OR b.y = 2 OR c.z = 3)".stripIndent(), text.sql());
    }
}
