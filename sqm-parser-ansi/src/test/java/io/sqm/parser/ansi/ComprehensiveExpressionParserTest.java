package io.sqm.parser.ansi;

import io.sqm.core.*;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for expression parsing covering various expression types.
 * This increases coverage for AtomicExprParser and related base parser classes.
 */
class ComprehensiveExpressionParserTest {

    private ParseContext ctx;

    @BeforeEach
    void setUp() {
        ctx = ParseContext.of(new AnsiSpecs());
    }

    @Test
    void parsesSimpleLiteral() {
        var result = ctx.parse(Expression.class, "42");
        assertTrue(result.ok());
        var expr = result.value();
        assertInstanceOf(LiteralExpr.class, expr);
        assertEquals(42L, ((LiteralExpr) expr).value());
    }

    @Test
    void parsesStringLiteral() {
        var result = ctx.parse(Expression.class, "'hello world'");
        assertTrue(result.ok());
        var expr = result.value();
        assertInstanceOf(LiteralExpr.class, expr);
        assertEquals("hello world", ((LiteralExpr) expr).value());
    }

    @Test
    void parsesBooleanLiterals() {
        var trueResult = ctx.parse(Expression.class, "TRUE");
        assertTrue(trueResult.ok());
        assertInstanceOf(LiteralExpr.class, trueResult.value());
        assertEquals(true, ((LiteralExpr) trueResult.value()).value());

        var falseResult = ctx.parse(Expression.class, "FALSE");
        assertTrue(falseResult.ok());
        assertInstanceOf(LiteralExpr.class, falseResult.value());
        assertEquals(false, ((LiteralExpr) falseResult.value()).value());
    }

    @Test
    void parsesNullLiteral() {
        var result = ctx.parse(Expression.class, "NULL");
        assertTrue(result.ok());
        var expr = result.value();
        assertInstanceOf(LiteralExpr.class, expr);
        assertNull(((LiteralExpr) expr).value());
    }

    @Test
    void parsesColumnReference() {
        var result = ctx.parse(Expression.class, "user_id");
        assertTrue(result.ok());
        var expr = result.value();
        assertInstanceOf(ColumnExpr.class, expr);
        assertEquals("user_id", ((ColumnExpr) expr).name());
    }

    @Test
    void parsesQualifiedColumnReference() {
        var result = ctx.parse(Expression.class, "users.user_id");
        assertTrue(result.ok());
        var expr = result.value();
        assertInstanceOf(ColumnExpr.class, expr);
        ColumnExpr col = (ColumnExpr) expr;
        assertEquals("users", col.tableAlias());
        assertEquals("user_id", col.name());
    }

    @Test
    void parsesFunctionCallNoArgs() {
        var result = ctx.parse(Expression.class, "NOW()");
        assertTrue(result.ok());
        var expr = result.value();
        assertInstanceOf(FunctionExpr.class, expr);
        FunctionExpr func = (FunctionExpr) expr;
        assertEquals("now", func.name().toLowerCase());
        assertTrue(func.args().isEmpty());
    }

    @Test
    void parsesFunctionCallWithArgs() {
        var result = ctx.parse(Expression.class, "CONCAT('Hello', ' ', 'World')");
        assertTrue(result.ok());
        var expr = result.value();
        assertInstanceOf(FunctionExpr.class, expr);
        FunctionExpr func = (FunctionExpr) expr;
        assertEquals("concat", func.name().toLowerCase());
        assertEquals(3, func.args().size());
    }

    @Test
    void parsesFunctionCallWithStarArg() {
        var result = ctx.parse(Expression.class, "COUNT(*)");
        assertTrue(result.ok());
        var expr = result.value();
        assertInstanceOf(FunctionExpr.class, expr);
        FunctionExpr func = (FunctionExpr) expr;
        assertEquals("count", func.name().toLowerCase());
        assertEquals(1, func.args().size());
        assertInstanceOf(FunctionExpr.Arg.StarArg.class, func.args().getFirst());
    }

    @Test
    void parsesFunctionCallWithDistinct() {
        var result = ctx.parse(Expression.class, "COUNT(DISTINCT user_id)");
        assertTrue(result.ok());
        var expr = result.value();
        assertInstanceOf(FunctionExpr.class, expr);
        FunctionExpr func = (FunctionExpr) expr;
        assertEquals("count", func.name().toLowerCase());
        assertTrue(func.distinctArg() != null && func.distinctArg());
    }

    @Test
    void parsesCaseExpression() {
        var result = ctx.parse(Expression.class, "CASE WHEN age > 18 THEN 'adult' ELSE 'minor' END");
        assertTrue(result.ok());
        var expr = result.value();
        assertInstanceOf(CaseExpr.class, expr);
        CaseExpr caseExpr = (CaseExpr) expr;
        assertEquals(1, caseExpr.whens().size());
        assertNotNull(caseExpr.elseExpr());
    }

    @Test
    void parsesCaseExpressionMultipleWhens() {
        var result = ctx.parse(Expression.class, 
            "CASE WHEN age < 13 THEN 'child' WHEN age < 18 THEN 'teen' ELSE 'adult' END");
        assertTrue(result.ok());
        var expr = result.value();
        assertInstanceOf(CaseExpr.class, expr);
        CaseExpr caseExpr = (CaseExpr) expr;
        assertEquals(2, caseExpr.whens().size());
    }

    @Test
    void parsesNestedFunctionCalls() {
        var result = ctx.parse(Expression.class, "UPPER(LOWER('TEST'))");
        assertTrue(result.ok());
        var expr = result.value();
        assertInstanceOf(FunctionExpr.class, expr);
        FunctionExpr outerFunc = (FunctionExpr) expr;
        assertEquals("upper", outerFunc.name().toLowerCase());
    }

    @Test
    void parsesParenthesizedExpression() {
        var result = ctx.parse(Expression.class, "(user_id)");
        assertTrue(result.ok());
        var expr = result.value();
        assertInstanceOf(ColumnExpr.class, expr);
        assertEquals("user_id", ((ColumnExpr) expr).name());
    }

    @Test
    void parsesArithmeticAddition() {
        var result = ctx.parse(Expression.class, "1 + 2");
        assertTrue(result.ok());
        var expr = result.value();
        assertInstanceOf(AddArithmeticExpr.class, expr);
    }

    @Test
    void parsesArithmeticSubtraction() {
        var result = ctx.parse(Expression.class, "10 - 5");
        assertTrue(result.ok());
        var expr = result.value();
        assertInstanceOf(SubArithmeticExpr.class, expr);
    }

    @Test
    void parsesArithmeticMultiplication() {
        var result = ctx.parse(Expression.class, "5 * 3");
        assertTrue(result.ok());
        var expr = result.value();
        assertInstanceOf(MulArithmeticExpr.class, expr);
    }

    @Test
    void parsesArithmeticDivision() {
        var result = ctx.parse(Expression.class, "20 / 4");
        assertTrue(result.ok());
        var expr = result.value();
        assertInstanceOf(DivArithmeticExpr.class, expr);
    }

    @Test
    void parsesArithmeticModulo() {
        var result = ctx.parse(Expression.class, "10 % 3");
        assertTrue(result.ok());
        var expr = result.value();
        assertInstanceOf(ModArithmeticExpr.class, expr);
    }

    @Test
    void parsesNegativeExpression() {
        var result = ctx.parse(Expression.class, "-42");
        assertTrue(result.ok());
        var expr = result.value();
        assertInstanceOf(NegativeArithmeticExpr.class, expr);
    }

    @Test
    void parsesComplexArithmeticWithPrecedence() {
        var result = ctx.parse(Expression.class, "2 + 3 * 4");
        assertTrue(result.ok());
        var expr = result.value();
        // Should parse as 2 + (3 * 4) due to precedence
        assertInstanceOf(AddArithmeticExpr.class, expr);
    }

    @Test
    void parsesCastExpression() {
        var result = ctx.parse(Expression.class, "CAST('123' AS INTEGER)");
        assertTrue(result.ok());
        var expr = result.value();
        assertInstanceOf(CastExpr.class, expr);
        CastExpr cast = (CastExpr) expr;
        assertNotNull(cast.type());
    }

    @Test
    void parsesSubqueryExpression() {
        var result = ctx.parse(Expression.class, "(SELECT 1)");
        assertTrue(result.ok());
        var expr = result.value();
        assertInstanceOf(QueryExpr.class, expr);
    }

    @Test
    void parsesRowConstructor() {
        var result = ctx.parse(Expression.class, "(1, 2, 3)");
        assertTrue(result.ok());
        var expr = result.value();
        assertInstanceOf(RowExpr.class, expr);
        RowExpr row = (RowExpr) expr;
        assertEquals(3, row.items().size());
    }
}
