package io.sqm.parser.ansi;

import io.sqm.core.CollateExpr;
import io.sqm.core.Expression;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests postfix parsing behavior for COLLATE expressions.
 */
class PostfixExprParserCollateTest {

    @Test
    void postfix_parses_collate_when_feature_enabled() {
        var ctx = ParseContext.of(new TestSpecs());
        var result = ctx.parse(Expression.class, "name COLLATE de_CH");
        assertTrue(result.ok());
        assertInstanceOf(CollateExpr.class, result.value());
    }

    @Test
    void postfix_rejects_collate_when_feature_disabled() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(Expression.class, "name COLLATE de_CH");
        assertTrue(result.isError());
    }
}
