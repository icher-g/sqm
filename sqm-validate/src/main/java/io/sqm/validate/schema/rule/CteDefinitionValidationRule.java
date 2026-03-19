package io.sqm.validate.schema.rule;

import io.sqm.core.CteDef;
import io.sqm.core.DeleteStatement;
import io.sqm.core.InsertStatement;
import io.sqm.core.Query;
import io.sqm.core.Statement;
import io.sqm.core.UpdateStatement;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;

import java.util.Optional;

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
        var projectionArity = projectionArity(node.body());
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

    /**
     * Returns projection arity for query-backed or writable CTE bodies.
     *
     * @param body CTE body statement.
     * @return projection arity when determinable.
     */
    private Optional<Integer> projectionArity(Statement body) {
        if (body instanceof Query query) {
            return projectionShapeInspector.projectionArity(query);
        }
        if (body instanceof InsertStatement insert) {
            return insert.result() == null ? Optional.empty() : Optional.of(insert.result().items().size());
        }
        if (body instanceof UpdateStatement update) {
            return update.result() == null ? Optional.empty() : Optional.of(update.result().items().size());
        }
        if (body instanceof DeleteStatement delete) {
            return delete.result() == null ? Optional.empty() : Optional.of(delete.result().items().size());
        }
        return Optional.empty();
    }
}
