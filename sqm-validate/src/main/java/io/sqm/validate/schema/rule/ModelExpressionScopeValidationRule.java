package io.sqm.validate.schema.rule;

import io.sqm.core.*;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;

/**
 * Rejects MODEL-only expressions outside the current query block's MODEL rules.
 */
final class ModelExpressionScopeValidationRule implements SchemaValidationRule<Expression> {
    /**
     * {@inheritDoc}
     */
    @Override
    public Class<Expression> nodeType() {
        return Expression.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate(Expression expression, SchemaValidationContext context) {
        boolean model = expression instanceof CellRefExpr || expression instanceof ModelAggregateExpr
            || expression instanceof CurrentDimensionExpr || expression instanceof IterationNumberExpr
            || expression instanceof PreviousModelValueExpr || expression instanceof PresenceValueExpr
            || expression instanceof CellPresentPredicate || expression instanceof IsAnyPredicate;
        if (model && context.modelScope() == null) {
            context.addProblem(ValidationProblem.Code.DIALECT_CLAUSE_INVALID, "MODEL expression requires the current query's MODEL rule scope", expression, "expression.model");
        }
    }
}
