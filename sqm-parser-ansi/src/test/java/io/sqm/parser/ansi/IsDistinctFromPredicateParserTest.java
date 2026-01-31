package io.sqm.parser.ansi;

import io.sqm.core.*;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.IdentifierQuoting;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link IsDistinctFromPredicateParser}.
 */
class IsDistinctFromPredicateParserTest {

    private ParseContext ctx;

    @BeforeEach
    void setUp() {
        ctx = ParseContext.of(new AnsiSpecs());
    }

    @Test
    void testParseIsDistinctFrom() {
        var sql = "a IS DISTINCT FROM b";
        var result = ctx.parse(IsDistinctFromPredicate.class, sql);

        assertTrue(result.ok());
        var predicate = result.value();
        assertNotNull(predicate);
        assertFalse(predicate.negated());

        assertInstanceOf(ColumnExpr.class, predicate.lhs());
        assertEquals("a", ((ColumnExpr) predicate.lhs()).name());

        assertInstanceOf(ColumnExpr.class, predicate.rhs());
        assertEquals("b", ((ColumnExpr) predicate.rhs()).name());
    }

    @Test
    void testParseIsNotDistinctFrom() {
        var sql = "x IS NOT DISTINCT FROM y";
        var result = ctx.parse(IsDistinctFromPredicate.class, sql);

        assertTrue(result.ok());
        var predicate = result.value();
        assertNotNull(predicate);
        assertTrue(predicate.negated());

        assertInstanceOf(ColumnExpr.class, predicate.lhs());
        assertEquals("x", ((ColumnExpr) predicate.lhs()).name());

        assertInstanceOf(ColumnExpr.class, predicate.rhs());
        assertEquals("y", ((ColumnExpr) predicate.rhs()).name());
    }

    @Test
    void testParseWithLiteral() {
        var sql = "status IS DISTINCT FROM 'active'";
        var result = ctx.parse(IsDistinctFromPredicate.class, sql);

        assertTrue(result.ok());
        var predicate = result.value();
        assertNotNull(predicate);
        assertFalse(predicate.negated());

        assertInstanceOf(ColumnExpr.class, predicate.lhs());
        assertEquals("status", ((ColumnExpr) predicate.lhs()).name());

        assertInstanceOf(LiteralExpr.class, predicate.rhs());
        assertEquals("active", ((LiteralExpr) predicate.rhs()).value());
    }

    @Test
    void testParseWithNull() {
        var sql = "col IS DISTINCT FROM NULL";
        var result = ctx.parse(IsDistinctFromPredicate.class, sql);

        assertTrue(result.ok());
        var predicate = result.value();
        assertNotNull(predicate);

        assertInstanceOf(LiteralExpr.class, predicate.rhs());
        assertNull(((LiteralExpr) predicate.rhs()).value());
    }

    @Test
    void testParseWithNumericLiteral() {
        var sql = "amount IS NOT DISTINCT FROM 100";
        var result = ctx.parse(IsDistinctFromPredicate.class, sql);

        assertTrue(result.ok());
        var predicate = result.value();
        assertTrue(predicate.negated());

        assertInstanceOf(LiteralExpr.class, predicate.rhs());
        assertEquals(100L, ((LiteralExpr) predicate.rhs()).value());
    }

    @Test
    void testParseWithQualifiedColumn() {
        var sql = "t1.id IS DISTINCT FROM t2.id";
        var result = ctx.parse(IsDistinctFromPredicate.class, sql);

        assertTrue(result.ok());
        var predicate = result.value();

        var lhs = (ColumnExpr) predicate.lhs();
        assertEquals("t1", lhs.tableAlias());
        assertEquals("id", lhs.name());

        var rhs = (ColumnExpr) predicate.rhs();
        assertEquals("t2", rhs.tableAlias());
        assertEquals("id", rhs.name());
    }

    @Test
    void testParseWithExpression() {
        var sql = "a + 1 IS DISTINCT FROM b * 2";
        var result = ctx.parse(IsDistinctFromPredicate.class, sql);

        assertTrue(result.ok());
        assertNotNull(result.value());
    }

    @Test
    void testParserImplementsCorrectInterfaces() {
        var parser = new IsDistinctFromPredicateParser();

        assertEquals(IsDistinctFromPredicate.class, parser.targetType());
    }

    @Test
    void testInfixParsing() {
        var parser = new IsDistinctFromPredicateParser();
        var lhs = ColumnExpr.of("a");
        var cur = Cursor.of("IS DISTINCT FROM b", IdentifierQuoting.of('"'));

        var result = parser.parse(lhs, cur, ctx);

        assertTrue(result.ok());
        var predicate = result.value();
        assertSame(lhs, predicate.lhs());
        assertFalse(predicate.negated());
    }

    @Test
    void testParseError_MissingDistinct() {
        var sql = "a IS FROM b";
        var result = ctx.parse(IsDistinctFromPredicate.class, sql);

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("DISTINCT"));
    }

    @Test
    void testParseError_MissingFrom() {
        var sql = "a IS DISTINCT b";
        var result = ctx.parse(IsDistinctFromPredicate.class, sql);

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("FROM"));
    }

    @Test
    void testParseError_MissingRhs() {
        var sql = "a IS DISTINCT FROM";
        var result = ctx.parse(IsDistinctFromPredicate.class, sql);

        assertTrue(result.isError());
    }

    @Test
    void testParseWithQuestionOperatorExpressionLhs() {
        var sql = "??? IS DISTINCT FROM 1";
        var result = ctx.parse(IsDistinctFromPredicate.class, sql);

        assertTrue(result.ok());
        var predicate = result.value();
        assertNotNull(predicate);

        assertInstanceOf(BinaryOperatorExpr.class, predicate.lhs());
        var lhs = (BinaryOperatorExpr) predicate.lhs();
        assertEquals("?", lhs.operator());
        assertInstanceOf(AnonymousParamExpr.class, lhs.left());
        assertInstanceOf(AnonymousParamExpr.class, lhs.right());

        assertInstanceOf(LiteralExpr.class, predicate.rhs());
        assertEquals(1L, ((LiteralExpr) predicate.rhs()).value());
    }

    @Test
    void testCaseInsensitive() {
        var sql = "a is DiStInCt FrOm b";
        var result = ctx.parse(IsDistinctFromPredicate.class, sql);

        assertTrue(result.ok());
        assertNotNull(result.value());
    }

    @Test
    void testWithParentheses() {
        var sql = "(a) IS DISTINCT FROM (b)";
        var result = ctx.parse(IsDistinctFromPredicate.class, sql);

        assertTrue(result.ok());
        assertNotNull(result.value());
    }

    @Test
    void testParseWithFunctionCall() {
        var sql = "UPPER(name) IS DISTINCT FROM 'JOHN'";
        var result = ctx.parse(IsDistinctFromPredicate.class, sql);

        assertTrue(result.ok());
        assertNotNull(result.value());
    }

    @Test
    void testParseMultipleInWhereClause() {
        var sql = "SELECT * FROM t WHERE a IS DISTINCT FROM b AND c IS NOT DISTINCT FROM d";
        var result = ctx.parse(io.sqm.core.Query.class, sql);

        assertTrue(result.ok(), () -> "Query should parse successfully but failed with: " + result.errorMessage());
        assertNotNull(result.value());
    }
}

