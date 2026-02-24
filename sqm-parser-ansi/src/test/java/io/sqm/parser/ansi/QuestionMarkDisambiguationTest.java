package io.sqm.parser.ansi;

import io.sqm.core.*;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuestionMarkDisambiguationTest {

    private SelectQuery parseSelect(String sql) {
        var ctx = ParseContext.of(new AnsiSpecs());
        var pr = ctx.parse(Query.class, sql);
        assertTrue(pr.ok(), () -> "Expected OK but got: " + pr);
        assertInstanceOf(SelectQuery.class, pr.value(), "Expected SelectQuery");
        return (SelectQuery) pr.value();
    }

    private Expression singleWhereExpr(SelectQuery q) {
        assertNotNull(q.where(), "Expected WHERE clause");
        assertInstanceOf(UnaryPredicate.class, q.where(), "Expected unary predicate fallback");
        return ((UnaryPredicate) q.where()).expr();
    }

    @Test
    void parsesAnonymousParamInWhere() {
        SelectQuery q = parseSelect("SELECT * FROM t WHERE ?");

        Expression expr = singleWhereExpr(q);
        assertInstanceOf(AnonymousParamExpr.class, expr);
    }

    @Test
    void parsesJsonbQuestionOperatorInWhere() {
        SelectQuery q = parseSelect("SELECT * FROM t WHERE data ? 'key'");

        Expression expr = singleWhereExpr(q);
        assertInstanceOf(BinaryOperatorExpr.class, expr);

        BinaryOperatorExpr op = (BinaryOperatorExpr) expr;
        assertEquals("?", op.operator().text());
        assertInstanceOf(ColumnExpr.class, op.left());
        assertEquals("data", ((ColumnExpr) op.left()).name().value());
        assertInstanceOf(LiteralExpr.class, op.right());
        assertEquals("key", ((LiteralExpr) op.right()).value());
    }

    @Test
    void parsesJsonbQuestionOperatorWithParamRhsInWhere() {
        SelectQuery q = parseSelect("SELECT * FROM t WHERE data ? ?");

        Expression expr = singleWhereExpr(q);
        assertInstanceOf(BinaryOperatorExpr.class, expr);

        BinaryOperatorExpr op = (BinaryOperatorExpr) expr;
        assertEquals("?", op.operator().text());
        assertInstanceOf(ColumnExpr.class, op.left());
        assertEquals("data", ((ColumnExpr) op.left()).name().value());
        assertInstanceOf(AnonymousParamExpr.class, op.right());
    }

    @Test
    void parsesQuestionOperatorWithParamLhsInWhere() {
        SelectQuery q = parseSelect("SELECT * FROM t WHERE ? ? 'key'");

        Expression expr = singleWhereExpr(q);
        assertInstanceOf(BinaryOperatorExpr.class, expr);

        BinaryOperatorExpr op = (BinaryOperatorExpr) expr;
        assertEquals("?", op.operator().text());
        assertInstanceOf(AnonymousParamExpr.class, op.left());
        assertInstanceOf(LiteralExpr.class, op.right());
        assertEquals("key", ((LiteralExpr) op.right()).value());
    }

    @Test
    void parsesQuestionOperatorWithParamLhsAndRhsInWhere() {
        SelectQuery q = parseSelect("SELECT * FROM t WHERE ? ? ?");

        Expression expr = singleWhereExpr(q);
        assertInstanceOf(BinaryOperatorExpr.class, expr);

        BinaryOperatorExpr op = (BinaryOperatorExpr) expr;
        assertEquals("?", op.operator().text());
        assertInstanceOf(AnonymousParamExpr.class, op.left());
        assertInstanceOf(AnonymousParamExpr.class, op.right());
    }

    @Test
    void parsesQuestionOperatorAndAnonymousParamInSelectList() {
        SelectQuery q = parseSelect("SELECT data ? 'key' AS has_key, ? AS flag FROM t");

        assertEquals(2, q.items().size());
        assertInstanceOf(ExprSelectItem.class, q.items().get(0));
        assertInstanceOf(ExprSelectItem.class, q.items().get(1));

        Expression first = ((ExprSelectItem) q.items().get(0)).expr();
        Expression second = ((ExprSelectItem) q.items().get(1)).expr();

        assertInstanceOf(BinaryOperatorExpr.class, first);
        assertInstanceOf(AnonymousParamExpr.class, second);
    }
}
