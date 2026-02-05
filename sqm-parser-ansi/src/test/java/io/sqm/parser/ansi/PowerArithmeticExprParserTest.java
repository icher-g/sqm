package io.sqm.parser.ansi;

import io.sqm.core.LiteralExpr;
import io.sqm.core.PowerArithmeticExpr;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PowerArithmeticExprParser}.
 */
class PowerArithmeticExprParserTest {

    @Test
    void parses_power_expression_when_feature_enabled() {
        var ctx = ParseContext.of(new TestSpecs());

        var result = ctx.parse(PowerArithmeticExpr.class, "2 ^ 3");

        assertTrue(result.ok());
        assertInstanceOf(PowerArithmeticExpr.class, result.value());
        var pow = result.value();
        assertInstanceOf(LiteralExpr.class, pow.lhs());
        assertInstanceOf(LiteralExpr.class, pow.rhs());
        assertEquals(2L, ((LiteralExpr) pow.lhs()).value());
        assertEquals(3L, ((LiteralExpr) pow.rhs()).value());
    }

    @Test
    void rejects_power_expression_when_feature_disabled() {
        var ctx = ParseContext.of(new AnsiSpecs());

        var result = ctx.parse(PowerArithmeticExpr.class, "2 ^ 3");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).toLowerCase().contains("not supported"));
    }
}
