package io.sqm.validate.schema.rule;

import io.sqm.core.ModelClause;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;

import java.util.Objects;

/**
 * Validates explicit dialect support for a typed MODEL clause.
 */
public final class ModelClauseFeatureValidationRule implements SchemaValidationRule<ModelClause> {
    private final String dialect;
    private final boolean supported;

    /**
     * Creates a MODEL capability rule.
     *
     * @param dialect   dialect name for diagnostics
     * @param supported whether this dialect supports the complete MODEL clause
     */
    public ModelClauseFeatureValidationRule(String dialect, boolean supported) {
        this.dialect = Objects.requireNonNull(dialect, "dialect");
        this.supported = supported;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<ModelClause> nodeType() {
        return ModelClause.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate(ModelClause model, SchemaValidationContext context) {
        if (!supported) context.addProblem(ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
            dialect + " does not support MODEL", model, "model");
    }
}
