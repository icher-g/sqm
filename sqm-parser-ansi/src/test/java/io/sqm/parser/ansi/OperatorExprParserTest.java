package io.sqm.parser.ansi;

import io.sqm.core.*;
import io.sqm.parser.core.ParserException;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for parsing generic operator expressions:
 * {@link BinaryOperatorExpr} and {@link UnaryOperatorExpr}.
 *
 * <p>These tests focus on AST shape, left-associativity, and precedence relative
 * to arithmetic parsing.</p>
 */
public class OperatorExprParserTest {

    @Test
    void parses_binary_operator_expr_simple() {
        var e = parseExpr("payload -> 'user'");

        assertInstanceOf(BinaryOperatorExpr.class, e);
        var op = (BinaryOperatorExpr) e;

        assertEquals("->", op.operator().text());
        assertInstanceOf(ColumnExpr.class, op.left());
        assertEquals("payload", ((ColumnExpr) op.left()).name().value());

        assertInstanceOf(LiteralExpr.class, op.right());
        assertEquals("user", ((LiteralExpr) op.right()).value());
    }

    @Test
    void parses_binary_operator_expr_left_associative_chain() {
        var e = parseExpr("payload -> 'a' ->> 'b'");

        // (payload -> 'a') ->> 'b'
        assertInstanceOf(BinaryOperatorExpr.class, e);
        var outer = (BinaryOperatorExpr) e;
        assertEquals("->>", outer.operator().text());

        assertInstanceOf(BinaryOperatorExpr.class, outer.left());
        var inner = (BinaryOperatorExpr) outer.left();
        assertEquals("->", inner.operator().text());

        assertInstanceOf(ColumnExpr.class, inner.left());
        assertEquals("payload", ((ColumnExpr) inner.left()).name().value());

        assertInstanceOf(LiteralExpr.class, inner.right());
        assertEquals("a", ((LiteralExpr) inner.right()).value());

        assertInstanceOf(LiteralExpr.class, outer.right());
        assertEquals("b", ((LiteralExpr) outer.right()).value());
    }

    @Test
    void binary_operator_binds_looser_than_arithmetic() {
        var e = parseExpr("a + b ->> 'x'");

        assertInstanceOf(BinaryOperatorExpr.class, e);
        var op = (BinaryOperatorExpr) e;
        assertEquals("->>", op.operator().text());

        // left side should be the full arithmetic expression (a + b)
        assertInstanceOf(AddArithmeticExpr.class, op.left());
        var add = (AddArithmeticExpr) op.left();
        assertInstanceOf(ColumnExpr.class, add.lhs());
        assertEquals("a", ((ColumnExpr) add.lhs()).name().value());
        assertInstanceOf(ColumnExpr.class, add.rhs());
        assertEquals("b", ((ColumnExpr) add.rhs()).name().value());

        assertInstanceOf(LiteralExpr.class, op.right());
        assertEquals("x", ((LiteralExpr) op.right()).value());
    }

    @Test
    void parses_unary_operator_expr_bitwise_not() {
        var e = parseExpr("~mask");

        assertInstanceOf(UnaryOperatorExpr.class, e);
        var op = (UnaryOperatorExpr) e;

        assertEquals("~", op.operator().text());
        assertInstanceOf(ColumnExpr.class, op.expr());
        assertEquals("mask", ((ColumnExpr) op.expr()).name().value());
    }

    private Expression parseExpr(String sql) {
        var ctx = ParseContext.of(new AnsiSpecs());
        var res = ctx.parse(Expression.class, sql);
        if (res.isError()) {
            throw new ParserException(res.errorMessage(), 0);
        }
        return res.value();
    }
}
