package io.sqm.parser.ansi;

import io.sqm.core.MulArithmeticExpr;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link MulArithmeticExprParser}.
 */
class MulArithmeticExprParserTest {

    @Test
    void parses_multiplication_expression() {
        var ctx = ParseContext.of(new AnsiSpecs());

        var result = ctx.parse(MulArithmeticExpr.class, "10 * 2");

        assertTrue(result.ok());
        assertInstanceOf(MulArithmeticExpr.class, result.value());
    }

    @Test
    void errors_when_rhs_is_missing() {
        var ctx = ParseContext.of(new AnsiSpecs());

        var result = ctx.parse(MulArithmeticExpr.class, "10 *");

        assertTrue(result.isError());
    }
}
