package io.sqm.parser.postgresql;

import io.sqm.core.ColumnExpr;
import io.sqm.core.Expression;
import io.sqm.core.MulArithmeticExpr;
import io.sqm.core.PowerArithmeticExpr;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parser tests for PostgreSQL exponentiation operator precedence.
 */
class PowerArithmeticExprParserTest {

    private ParseContext ctx;

    @BeforeEach
    void setUp() {
        ctx = ParseContext.of(new PostgresSpecs());
    }

    @Test
    void parses_exponentiation_operator() {
        var result = ctx.parse(Expression.class, "a ^ b");
        assertTrue(result.ok());

        var expr = assertInstanceOf(PowerArithmeticExpr.class, result.value());
        var lhs = assertInstanceOf(ColumnExpr.class, expr.lhs());
        var rhs = assertInstanceOf(ColumnExpr.class, expr.rhs());

        assertEquals("a", lhs.name());
        assertEquals("b", rhs.name());
    }

    @Test
    void exponentiation_is_left_associative() {
        var result = ctx.parse(Expression.class, "a ^ b ^ c");
        assertTrue(result.ok());

        var outer = assertInstanceOf(PowerArithmeticExpr.class, result.value());
        var inner = assertInstanceOf(PowerArithmeticExpr.class, outer.lhs());
        var rhs = assertInstanceOf(ColumnExpr.class, outer.rhs());

        var innerLhs = assertInstanceOf(ColumnExpr.class, inner.lhs());
        var innerRhs = assertInstanceOf(ColumnExpr.class, inner.rhs());

        assertEquals("a", innerLhs.name());
        assertEquals("b", innerRhs.name());
        assertEquals("c", rhs.name());
    }

    @Test
    void exponentiation_has_higher_precedence_than_multiplication() {
        var result = ctx.parse(Expression.class, "a * b ^ c");
        assertTrue(result.ok());

        var expr = assertInstanceOf(MulArithmeticExpr.class, result.value());
        var lhs = assertInstanceOf(ColumnExpr.class, expr.lhs());
        var rhs = assertInstanceOf(PowerArithmeticExpr.class, expr.rhs());

        var rhsLhs = assertInstanceOf(ColumnExpr.class, rhs.lhs());
        var rhsRhs = assertInstanceOf(ColumnExpr.class, rhs.rhs());

        assertEquals("a", lhs.name());
        assertEquals("b", rhsLhs.name());
        assertEquals("c", rhsRhs.name());
    }
}
