package io.sqm.render.ansi;

import io.sqm.core.Join;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NaturalJoinRendererTest {

    private final RenderContext ctx = RenderContext.of(new AnsiDialect());

    @Test
    void rendersNaturalJoin() {
        Join join = Join.natural(tbl("t"));
        assertEquals("NATURAL JOIN t", ctx.render(join).sql());
    }
}
