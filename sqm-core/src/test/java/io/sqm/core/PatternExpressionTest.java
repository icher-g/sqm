package io.sqm.core;

import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class PatternExpressionTest {
    @Test
    void dslBuildsEveryScopedExpressionWithoutGenericFunctionFallbacks() {
        var column = patternColumn("A", "amount");
        var classifier = classifier();
        var qualifiedClassifier = classifier("A");
        var number = matchNumber();
        var navigation = prev(last(column, 2), 1);
        var running = running(navigation);
        var finalValue = finalValue(first(column));

        assertEquals("A", column.variable().value());
        assertNull(classifier.variable());
        assertEquals("A", qualifiedClassifier.variable().value());
        assertSame(number, matchNumber());
        assertEquals(PatternNavigationExpr.Kind.PREV, navigation.kind());
        assertInstanceOf(PatternNavigationExpr.class, navigation.expression());
        assertEquals(1, ((LiteralExpr) navigation.offset()).value());
        assertEquals(PatternEvaluationExpr.Mode.RUNNING, running.mode());
        assertEquals(PatternEvaluationExpr.Mode.FINAL, finalValue.mode());

        assertFalse(column instanceof FunctionExpr);
        assertFalse(classifier instanceof FunctionExpr);
        assertFalse(number instanceof FunctionExpr);
        assertFalse(navigation instanceof FunctionExpr);
        assertFalse(running instanceof FunctionExpr);
    }

    @Test
    void scopedExpressionsValidateRequiredLocalState() {
        assertThrows(NullPointerException.class, () -> PatternColumnExpr.of(null, id("amount")));
        assertThrows(NullPointerException.class, () -> PatternColumnExpr.of(id("A"), null));
        assertThrows(NullPointerException.class, () -> PatternNavigationExpr.of(null, col("amount"), null));
        assertThrows(NullPointerException.class, () -> PatternNavigationExpr.of(PatternNavigationExpr.Kind.FIRST, null, null));
        assertThrows(NullPointerException.class, () -> PatternEvaluationExpr.of(null, col("amount")));
        assertThrows(NullPointerException.class, () -> PatternEvaluationExpr.of(PatternEvaluationExpr.Mode.FINAL, null));
    }

    @Test
    void expressionMatcherSelectsAllPatternExpressionBranches() {
        assertEquals(
            "column",
            patternColumn("A", "amount").<String>matchExpression()
                .patternColumn(value -> "column")
                .orElse("other")
        );
        assertEquals(
            "classifier",
            classifier().<String>matchExpression()
                .classifier(value -> "classifier")
                .orElse("other")
        );
        assertEquals(
            "number",
            matchNumber().<String>matchExpression()
                .matchNumber(value -> "number")
                .orElse("other")
        );
        assertEquals(
            "navigation",
            first(patternColumn("A", "amount")).<String>matchExpression()
                .patternNavigation(value -> "navigation")
                .orElse("other")
        );
        assertEquals(
            "evaluation",
            running(first(patternColumn("A", "amount"))).<String>matchExpression()
                .patternEvaluation(value -> "evaluation")
                .orElse("other")
        );
    }
}
