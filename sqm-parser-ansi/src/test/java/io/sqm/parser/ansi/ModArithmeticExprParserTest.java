package io.sqm.parser.ansi;

import io.sqm.core.Expression;
import io.sqm.core.ModArithmeticExpr;
import io.sqm.parser.AtomicExprParser;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class ModArithmeticExprParserTest {

    private final ParseContext ctx = ParseContext.of(new AnsiSpecs());
    private final ModArithmeticExprParser parser = new ModArithmeticExprParser(new AtomicExprParser());

    @Test
    void parsesModFunctionSyntax() {
        var result = ctx.parse(parser, "MOD(1, 2)");

        assertTrue(result.ok());
        assertInstanceOf(ModArithmeticExpr.class, result.value());
    }

    @Test
    void parsesInfixPercentSyntax() {
        var cur = Cursor.of("% 2", ctx.identifierQuoting());
        var lhs = Expression.literal(1);

        var result = parser.parse(lhs, cur, ctx);

        assertTrue(result.ok());
        assertInstanceOf(ModArithmeticExpr.class, result.value());
    }

    @Test
    void rejectsNonModIdentifier() {
        var result = ctx.parse(parser, "MAX(1,2)");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Expected MOD"));
    }
}
