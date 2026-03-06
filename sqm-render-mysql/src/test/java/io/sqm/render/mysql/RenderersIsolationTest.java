package io.sqm.render.mysql;

import io.sqm.core.LimitOffset;
import io.sqm.render.spi.RenderersRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class RenderersIsolationTest {

    @Test
    void mysqlRegistry_doesNotMutateAnsiRegistry() {
        RenderersRepository ansi = io.sqm.render.ansi.Renderers.ansi();
        RenderersRepository mysql = Renderers.mysql();

        assertInstanceOf(io.sqm.render.ansi.LimitOffsetRenderer.class, ansi.require(LimitOffset.class));
        assertInstanceOf(MySqlLimitOffsetRenderer.class, mysql.require(LimitOffset.class));
    }
}

