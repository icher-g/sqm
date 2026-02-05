package io.sqm.parser.postgresql;

import io.sqm.core.BinaryOperatorExpr;
import io.sqm.core.ColumnExpr;
import io.sqm.core.Expression;
import io.sqm.parser.core.ParserException;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.ParseProblem;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests PostgreSQL custom operator precedence tiers.
 */
public class CustomOperatorPrecedenceParserTest {

    @Test
    void higher_tier_operator_binds_tighter_than_lower_tier() {
        var expr = parseExpr("a || b ## c");

        assertInstanceOf(BinaryOperatorExpr.class, expr);
        var outer = (BinaryOperatorExpr) expr;
        assertEquals("||", outer.operator());

        assertInstanceOf(ColumnExpr.class, outer.left());
        assertEquals("a", ((ColumnExpr) outer.left()).name());

        assertInstanceOf(BinaryOperatorExpr.class, outer.right());
        var inner = (BinaryOperatorExpr) outer.right();
        assertEquals("##", inner.operator());

        assertInstanceOf(ColumnExpr.class, inner.left());
        assertEquals("b", ((ColumnExpr) inner.left()).name());
        assertInstanceOf(ColumnExpr.class, inner.right());
        assertEquals("c", ((ColumnExpr) inner.right()).name());
    }

    @Test
    void operator_syntax_always_uses_lowest_precedence() {
        var expr = parseExpr("a OPERATOR(pg_catalog.##) b ## c");

        assertInstanceOf(BinaryOperatorExpr.class, expr);
        var outer = (BinaryOperatorExpr) expr;
        assertEquals("OPERATOR(pg_catalog.##)", outer.operator());

        assertInstanceOf(ColumnExpr.class, outer.left());
        assertEquals("a", ((ColumnExpr) outer.left()).name());

        assertInstanceOf(BinaryOperatorExpr.class, outer.right());
        var inner = (BinaryOperatorExpr) outer.right();
        assertEquals("##", inner.operator());
    }

    @Test
    void same_tier_is_left_associative() {
        var expr = parseExpr("a ## b ## c");

        assertInstanceOf(BinaryOperatorExpr.class, expr);
        var outer = (BinaryOperatorExpr) expr;
        assertEquals("##", outer.operator());

        assertInstanceOf(BinaryOperatorExpr.class, outer.left());
        var inner = (BinaryOperatorExpr) outer.left();
        assertEquals("##", inner.operator());

        assertInstanceOf(ColumnExpr.class, inner.left());
        assertEquals("a", ((ColumnExpr) inner.left()).name());
        assertInstanceOf(ColumnExpr.class, inner.right());
        assertEquals("b", ((ColumnExpr) inner.right()).name());

        assertInstanceOf(ColumnExpr.class, outer.right());
        assertEquals("c", ((ColumnExpr) outer.right()).name());
    }

    @Test
    void recursion_binds_higher_precedence_inside_lower_precedence_chain() {
        var expr = parseExpr("a || b ## c || d");

        assertInstanceOf(BinaryOperatorExpr.class, expr);
        var outer = (BinaryOperatorExpr) expr;
        assertEquals("||", outer.operator());

        assertInstanceOf(BinaryOperatorExpr.class, outer.left());
        var left = (BinaryOperatorExpr) outer.left();
        assertEquals("||", left.operator());

        assertInstanceOf(ColumnExpr.class, left.left());
        assertEquals("a", ((ColumnExpr) left.left()).name());

        assertInstanceOf(BinaryOperatorExpr.class, left.right());
        var nested = (BinaryOperatorExpr) left.right();
        assertEquals("##", nested.operator());

        assertInstanceOf(ColumnExpr.class, nested.left());
        assertEquals("b", ((ColumnExpr) nested.left()).name());
        assertInstanceOf(ColumnExpr.class, nested.right());
        assertEquals("c", ((ColumnExpr) nested.right()).name());

        assertInstanceOf(ColumnExpr.class, outer.right());
        assertEquals("d", ((ColumnExpr) outer.right()).name());
    }

    @Test
    void higher_precedence_operator_binds_inside_medium_precedence() {
        var expr = parseExpr("a && b ## c");

        assertInstanceOf(BinaryOperatorExpr.class, expr);
        var outer = (BinaryOperatorExpr) expr;
        assertEquals("&&", outer.operator());

        assertInstanceOf(ColumnExpr.class, outer.left());
        assertEquals("a", ((ColumnExpr) outer.left()).name());

        assertInstanceOf(BinaryOperatorExpr.class, outer.right());
        var inner = (BinaryOperatorExpr) outer.right();
        assertEquals("##", inner.operator());

        assertInstanceOf(ColumnExpr.class, inner.left());
        assertEquals("b", ((ColumnExpr) inner.left()).name());
        assertInstanceOf(ColumnExpr.class, inner.right());
        assertEquals("c", ((ColumnExpr) inner.right()).name());
    }

    @Test
    void lower_precedence_operator_binds_after_higher_precedence() {
        var expr = parseExpr("a ## b && c");

        assertInstanceOf(BinaryOperatorExpr.class, expr);
        var outer = (BinaryOperatorExpr) expr;
        assertEquals("&&", outer.operator());

        assertInstanceOf(BinaryOperatorExpr.class, outer.left());
        var inner = (BinaryOperatorExpr) outer.left();
        assertEquals("##", inner.operator());

        assertInstanceOf(ColumnExpr.class, inner.left());
        assertEquals("a", ((ColumnExpr) inner.left()).name());
        assertInstanceOf(ColumnExpr.class, inner.right());
        assertEquals("b", ((ColumnExpr) inner.right()).name());

        assertInstanceOf(ColumnExpr.class, outer.right());
        assertEquals("c", ((ColumnExpr) outer.right()).name());
    }

    @Test
    void low_precedence_chain_is_left_associative() {
        var expr = parseExpr("a || b || c");

        assertInstanceOf(BinaryOperatorExpr.class, expr);
        var outer = (BinaryOperatorExpr) expr;
        assertEquals("||", outer.operator());

        assertInstanceOf(BinaryOperatorExpr.class, outer.left());
        var inner = (BinaryOperatorExpr) outer.left();
        assertEquals("||", inner.operator());

        assertInstanceOf(ColumnExpr.class, inner.left());
        assertEquals("a", ((ColumnExpr) inner.left()).name());
        assertInstanceOf(ColumnExpr.class, inner.right());
        assertEquals("b", ((ColumnExpr) inner.right()).name());

        assertInstanceOf(ColumnExpr.class, outer.right());
        assertEquals("c", ((ColumnExpr) outer.right()).name());
    }

    @Test
    void parse_without_operator_returns_lhs() {
        var expr = parseExpr("a");

        assertInstanceOf(ColumnExpr.class, expr);
        assertEquals("a", ((ColumnExpr) expr).name());
    }

    @Test
    void infix_parse_without_operator_returns_error() {
        var parser = new BinaryOperatorExprParser();
        var ctx = ParseContext.of(new PostgresSpecs());
        var cur = Cursor.of("", ctx.identifierQuoting());

        var result = parser.parse(ColumnExpr.of("a"), cur, ctx);
        assertInstanceOf(ParseProblem.class, result.problems().getFirst());
        assertTrue(Objects.requireNonNull(result.errorMessage()).startsWith("Expected operator"));
    }

    private Expression parseExpr(String sql) {
        var ctx = ParseContext.of(new PostgresSpecs());
        var res = ctx.parse(Expression.class, sql);
        if (res.isError()) {
            throw new ParserException(res.errorMessage(), 0);
        }
        return res.value();
    }
}