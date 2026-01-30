package io.sqm.render.defaults;

import io.sqm.render.RenderTestDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultRenderContextTest {

    @Test
    void storesDialect() {
        var dialect = new RenderTestDialect();
        var ctx = RenderContext.of(dialect);

        assertSame(dialect, ctx.dialect());
        assertInstanceOf(DefaultRenderContext.class, ctx);
    }
}
