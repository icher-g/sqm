package io.sqm.render.ansi;

import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.star;
import static org.junit.jupiter.api.Assertions.assertEquals;

class QualifiedStarSelectItemRendererTest {

    private final RenderContext ctx = RenderContext.of(new AnsiDialect());

    @Test
    void rendersStarSelectItem() {
        assertEquals("*", ctx.render(star()).sql());
    }

    @Test
    void rendersQualifiedStarSelectItem() {
        assertEquals("t.*", ctx.render(star("t")).sql());
    }
}
