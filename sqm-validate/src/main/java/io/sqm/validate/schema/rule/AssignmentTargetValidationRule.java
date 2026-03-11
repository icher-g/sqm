package io.sqm.validate.schema.rule;

import io.sqm.core.Assignment;
import io.sqm.validate.schema.internal.SchemaValidationContext;

/**
 * Validates DML assignment target columns against the current DML scope.
 */
public final class AssignmentTargetValidationRule implements SchemaValidationRule<Assignment> {

    /**
     * Creates an assignment-target validation rule.
     */
    public AssignmentTargetValidationRule() {
    }

    @Override
    public Class<Assignment> nodeType() {
        return Assignment.class;
    }

    @Override
    public void validate(Assignment node, SchemaValidationContext context) {
        context.resolveCurrentScopeColumn(node.column(), true, node, "dml.assignment");
    }
}
