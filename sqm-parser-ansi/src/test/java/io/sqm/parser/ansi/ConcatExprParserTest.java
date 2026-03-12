package io.sqm.parser.ansi;

import io.sqm.core.BinaryOperatorExpr;
import io.sqm.core.ColumnExpr;
import io.sqm.core.ConcatExpr;
import io.sqm.core.Expression;
import io.sqm.core.LiteralExpr;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConcatExprParserTest {

    @Test
    void parsesDoublePipeConcatAsSemanticNode() {
        var ctx = ParseContext.of(new TestSpecs());
        var result = ctx.parse(Expression.class, "first_name || ' ' || last_name");

        assertTrue(result.ok(), result.errorMessage());

        var expr = assertInstanceOf(ConcatExpr.class, result.value());
        assertEquals(3, expr.args().size());
        assertEquals("first_name", assertInstanceOf(ColumnExpr.class, expr.args().getFirst()).name().value());
        assertEquals(" ", assertInstanceOf(LiteralExpr.class, expr.args().get(1)).value());
        assertEquals("last_name", assertInstanceOf(ColumnExpr.class, expr.args().get(2)).name().value());
    }

    @Test
    void returnsNonConcatExpressionUnchanged() {
        var ctx = ParseContext.of(new TestSpecs());
        var result = ctx.parse(Expression.class, "first_name");

        assertTrue(result.ok(), result.errorMessage());
        assertInstanceOf(ColumnExpr.class, result.value());
    }

    @Test
    void preservesNonConcatBinaryOperators() {
        var ctx = ParseContext.of(new TestSpecs());
        var result = ctx.parse(Expression.class, "first_name ? last_name");

        assertTrue(result.ok(), result.errorMessage());
        var expr = assertInstanceOf(BinaryOperatorExpr.class, result.value());
        assertEquals("?", expr.operator().text());
    }
}
