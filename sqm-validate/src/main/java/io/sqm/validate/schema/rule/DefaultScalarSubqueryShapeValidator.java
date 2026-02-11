package io.sqm.validate.schema.rule;

import io.sqm.core.Expression;
import io.sqm.core.Node;
import io.sqm.core.QueryExpr;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;

import java.util.Objects;

/**
 * Default {@link ScalarSubqueryShapeValidator} implementation.
 */
class DefaultScalarSubqueryShapeValidator implements ScalarSubqueryShapeValidator {
    private final ProjectionShapeInspector projectionShapeInspector;

    /**
     * Creates scalar-subquery shape validator.
     *
     * @param projectionShapeInspector projection shape inspector.
     */
    DefaultScalarSubqueryShapeValidator(ProjectionShapeInspector projectionShapeInspector) {
        this.projectionShapeInspector = Objects.requireNonNull(projectionShapeInspector, "projectionShapeInspector");
    }

    /**
     * Validates that expression is scalar when it is a subquery expression.
     *
     * @param expression expression to check.
     * @param owner owner node for diagnostics.
     * @param clausePath clause path for diagnostics.
     * @param context validation context.
     * @param contextLabel short label describing scalar context.
     * @return true when shape is valid or expression is not a subquery expression.
     */
    @Override
    public boolean ensureScalarSubquery(
        Expression expression,
        Node owner,
        String clausePath,
        SchemaValidationContext context,
        String contextLabel
    ) {
        if (!(expression instanceof QueryExpr queryExpr)) {
            return true;
        }
        if (projectionShapeInspector.isSingleExpressionProjection(queryExpr.subquery())) {
            return true;
        }
        context.addProblem(
            ValidationProblem.Code.SUBQUERY_SHAPE_MISMATCH,
            contextLabel + " subquery must project exactly one expression column",
            owner,
            clausePath
        );
        return false;
    }
}
