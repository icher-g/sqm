package io.sqm.validate.schema.rule;

import io.sqm.core.CteDef;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;

/**
 * Validates CTE definition metadata against CTE body shape.
 */
final class CteDefinitionValidationRule implements SchemaValidationRule<CteDef> {
    private final ProjectionShapeInspector projectionShapeInspector;

    /**
     * Creates CTE definition validation rule.
     *
     * @param projectionShapeInspector projection shape inspector.
     */
    CteDefinitionValidationRule(ProjectionShapeInspector projectionShapeInspector) {
        this.projectionShapeInspector = projectionShapeInspector;
    }

    /**
     * Returns supported node type.
     *
     * @return CTE definition type.
     */
    @Override
    public Class<CteDef> nodeType() {
        return CteDef.class;
    }

    /**
     * Validates explicit CTE column alias count.
     *
     * @param node CTE definition node.
     * @param context schema validation context.
     */
    @Override
    public void validate(CteDef node, SchemaValidationContext context) {
        if (node.columnAliases() == null || node.columnAliases().isEmpty() || node.body() == null) {
            return;
        }
        var projectionArity = projectionShapeInspector.projectionArity(node.body());
        if (projectionArity.isEmpty()) {
            return;
        }
        if (projectionArity.get() == node.columnAliases().size()) {
            return;
        }
        context.addProblem(
            ValidationProblem.Code.CTE_COLUMN_ALIAS_COUNT_MISMATCH,
            "CTE '" + node.name().value() + "' column alias count "
                + node.columnAliases().size()
                + " does not match body projection width "
                + projectionArity.get(),
            node,
            "cte.columns"
        );
    }
}
