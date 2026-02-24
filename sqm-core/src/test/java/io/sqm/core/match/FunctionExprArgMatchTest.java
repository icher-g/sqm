package io.sqm.core.match;

import io.sqm.core.FunctionExpr;
import org.junit.jupiter.api.Test;

import static io.sqm.core.Expression.*;
import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.func;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link FunctionExprArgMatch} and {@link FunctionExprArgMatchImpl}.
 */
public class FunctionExprArgMatchTest {

    @Test
    void match_exprArg_appliesExprArgHandler() {
        FunctionExpr.Arg exprArg = funcArg(literal(42));
        String result = FunctionExprArgMatch
            .<String>match(exprArg)
            .exprArg(arg -> "EXPR_ARG")
            .starArg(arg -> "STAR_ARG")
            .orElseThrow(() -> new RuntimeException("No handler matched"));
        assertEquals("EXPR_ARG", result);
    }

    @Test
    void match_starArg_appliesStarArgHandler() {
        FunctionExpr.Arg starArg = starArg();
        String result = FunctionExprArgMatch
            .<String>match(starArg)
            .exprArg(arg -> "EXPR_ARG")
            .starArg(arg -> "STAR_ARG")
            .orElseThrow(() -> new RuntimeException("No handler matched"));
        assertEquals("STAR_ARG", result);
    }

    @Test
    void match_exprArgWithComplexExpression() {
        FunctionExpr complexExpr = func("upper", funcArg(col("users", "name")));
        FunctionExpr.Arg exprArg = funcArg(complexExpr);
        
        String result = FunctionExprArgMatch
            .<String>match(exprArg)
            .exprArg(arg -> "Found expression")
            .starArg(arg -> "STAR")
            .orElseThrow(() -> new RuntimeException("No handler matched"));
        
        assertEquals("Found expression", result);
    }

    @Test
    void match_chainedMatchers_firstMatchWins() {
        FunctionExpr.Arg starArg = starArg();
        String result = FunctionExprArgMatch
            .<String>match(starArg)
            .starArg(arg -> "STAR")
            .exprArg(arg -> "EXPR")
            .orElseThrow(() -> new RuntimeException("No handler matched"));
        
        assertEquals("STAR", result);
    }

    @Test
    void match_exprArgWithColumnExpression() {
        FunctionExpr.Arg exprArg = funcArg(col("t", "id"));
        
        String result = FunctionExprArgMatch
            .<String>match(exprArg)
            .exprArg(arg -> "Column")
            .starArg(arg -> "Star")
            .orElseThrow(() -> new RuntimeException("No handler matched"));
        
        assertEquals("Column", result);
    }

    @Test
    void match_differentArgTypes_handledCorrectly() {
        FunctionExpr.Arg literalArg = funcArg(literal(100));
        FunctionExpr.Arg star = starArg();
        
        String result1 = FunctionExprArgMatch
            .<String>match(literalArg)
            .exprArg(arg -> "literal")
            .starArg(arg -> "star")
            .orElseThrow(() -> new RuntimeException("No handler matched"));
        
        String result2 = FunctionExprArgMatch
            .<String>match(star)
            .exprArg(arg -> "literal")
            .starArg(arg -> "star")
            .orElseThrow(() -> new RuntimeException("No handler matched"));
        
        assertEquals("literal", result1);
        assertEquals("star", result2);
    }

    @Test
    void match_multipleExprArgs_differentExpressions() {
        FunctionExpr.Arg arg1 = funcArg(col("t1", "c1"));
        FunctionExpr.Arg arg2 = funcArg(literal("test"));
        FunctionExpr.Arg arg3 = funcArg(func("count", starArg()));
        
        String result1 = FunctionExprArgMatch.<String>match(arg1)
            .exprArg(a -> "col")
            .starArg(a -> "star")
            .orElseThrow(() -> new RuntimeException("No handler matched"));
        
        String result2 = FunctionExprArgMatch.<String>match(arg2)
            .exprArg(a -> "lit")
            .starArg(a -> "star")
            .orElseThrow(() -> new RuntimeException("No handler matched"));
        
        String result3 = FunctionExprArgMatch.<String>match(arg3)
            .exprArg(a -> "func")
            .starArg(a -> "star")
            .orElseThrow(() -> new RuntimeException("No handler matched"));
        
        assertEquals("col", result1);
        assertEquals("lit", result2);
        assertEquals("func", result3);
    }
}




