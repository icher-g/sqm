package io.sqm.render.ansi;

import io.sqm.core.LimitOffset;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class RenderersCopyIsolationTest {

    @Test
    void ansiCopy_returns_isolated_repository() {
        var shared = Renderers.ansi();
        var copy = Renderers.ansiCopy();

        assertNotSame(shared, copy);
        assertInstanceOf(LimitOffsetRenderer.class, shared.require(LimitOffset.class));
        assertInstanceOf(LimitOffsetRenderer.class, copy.require(LimitOffset.class));

        copy.register(new Renderer<LimitOffset>() {
            @Override
            public Class<LimitOffset> targetType() {
                return LimitOffset.class;
            }

            @Override
            public void render(LimitOffset node, RenderContext ctx, SqlWriter w) {
                w.append("FORCED");
            }
        });

        assertInstanceOf(LimitOffsetRenderer.class, shared.require(LimitOffset.class));
    }
}

