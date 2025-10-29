package io.sqm.render.ansi;

import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class NotPredicateRendererTest {

    @Test
    void renders_NOT_with_single() {
        var only = col("t", "is_deleted").eq(false);
        var root = only.not();

        RenderContext ctx = RenderContext.of(new AnsiDialect());
        var text = ctx.render(root);

        assertEquals("NOT (t.is_deleted = FALSE)", text.sql());
    }
}
