package io.sqm.validate.schema.rule;

import io.sqm.core.ClassifierExpr;
import io.sqm.core.Expression;
import io.sqm.core.MatchNumberExpr;
import io.sqm.core.PatternColumnExpr;
import io.sqm.core.PatternEvaluationExpr;
import io.sqm.core.PatternNavigationExpr;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;

/**
 * Rejects row-pattern-only expressions outside measures and definitions.
 */
final class PatternExpressionScopeValidationRule implements SchemaValidationRule<Expression> {
    @Override
    public Class<Expression> nodeType() {
        return Expression.class;
    }

    @Override
    public void validate(Expression node, SchemaValidationContext context) {
        if (!isPatternExpression(node) || context.inPatternExpressionScope()) {
            return;
        }
        context.addProblem(
            ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
            "Row-pattern expression is only valid in MATCH_RECOGNIZE measures or definitions",
            node,
            context.patternExpressionPath()
        );
    }

    private static boolean isPatternExpression(Expression node) {
        return node instanceof PatternColumnExpr
            || node instanceof ClassifierExpr
            || node instanceof MatchNumberExpr
            || node instanceof PatternNavigationExpr
            || node instanceof PatternEvaluationExpr;
    }
}
