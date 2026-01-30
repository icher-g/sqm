package io.sqm.parser.ansi;

import io.sqm.core.BoundSpec;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoundSpecParserTest {

    private final ParseContext ctx = ParseContext.of(new AnsiSpecs());

    @Test
    void parsesUnboundedPrecedingAndFollowing() {
        assertTrue(ctx.parse(BoundSpec.class, "UNBOUNDED PRECEDING").ok());
        assertTrue(ctx.parse(BoundSpec.class, "UNBOUNDED FOLLOWING").ok());
    }

    @Test
    void parsesCurrentRowAndRelativeBounds() {
        var current = ctx.parse(BoundSpec.class, "CURRENT ROW");
        var preceding = ctx.parse(BoundSpec.class, "1 PRECEDING");
        var following = ctx.parse(BoundSpec.class, "2 FOLLOWING");

        assertTrue(current.ok());
        assertTrue(preceding.ok());
        assertTrue(following.ok());

        assertInstanceOf(BoundSpec.CurrentRow.class, current.value());
        assertInstanceOf(BoundSpec.Preceding.class, preceding.value());
        assertInstanceOf(BoundSpec.Following.class, following.value());
    }
}
