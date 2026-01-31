package io.sqm.parser.ansi;

import io.sqm.core.AnonymousParamExpr;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnonymousParamExprParserTest {

    private final ParseContext ctx = ParseContext.of(new AnsiSpecs());

    @Test
    void parsesQuestionMark() {
        var result = ctx.parse(AnonymousParamExpr.class, "?");

        assertTrue(result.ok());
        assertInstanceOf(AnonymousParamExpr.class, result.value());
    }

    @Test
    void rejectsNonQuestionMark() {
        var result = ctx.parse(AnonymousParamExpr.class, "1");

        assertTrue(result.isError());
    }
}
