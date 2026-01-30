package io.sqm.render.ansi;

import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.unboundedFollowing;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BoundSpecUnboundedFollowingRendererTest {

    private final RenderContext ctx = RenderContext.of(new AnsiDialect());

    @Test
    void rendersUnboundedFollowing() {
        assertEquals("UNBOUNDED FOLLOWING", ctx.render(unboundedFollowing()).sql());
    }
}
