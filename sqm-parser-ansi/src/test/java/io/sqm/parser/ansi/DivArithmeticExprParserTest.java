package io.sqm.parser.ansi;

import io.sqm.core.DivArithmeticExpr;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link DivArithmeticExprParser}.
 */
class DivArithmeticExprParserTest {

    @Test
    void parses_division_expression() {
        var ctx = ParseContext.of(new AnsiSpecs());

        var result = ctx.parse(DivArithmeticExpr.class, "10 / 2");

        assertTrue(result.ok());
        assertInstanceOf(DivArithmeticExpr.class, result.value());
    }

    @Test
    void errors_when_rhs_is_missing() {
        var ctx = ParseContext.of(new AnsiSpecs());

        var result = ctx.parse(DivArithmeticExpr.class, "10 /");

        assertTrue(result.isError());
    }
}
