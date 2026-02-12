package io.sqm.validate.postgresql.rule;

import io.sqm.core.CteDef;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.rule.SchemaValidationRule;

import java.util.Objects;

/**
 * Validates PostgreSQL CTE features against versioned dialect capabilities.
 */
public final class PostgresCteFeatureValidationRule implements SchemaValidationRule<CteDef> {
    private final DialectCapabilities capabilities;
    private final SqlDialectVersion version;

    /**
     * Creates a PostgreSQL CTE feature validation rule.
     *
     * @param capabilities dialect capabilities.
     * @param version PostgreSQL version.
     */
    public PostgresCteFeatureValidationRule(DialectCapabilities capabilities, SqlDialectVersion version) {
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
        this.version = Objects.requireNonNull(version, "version");
    }

    @Override
    public Class<CteDef> nodeType() {
        return CteDef.class;
    }

    @Override
    public void validate(CteDef node, SchemaValidationContext context) {
        if (node.materialization() != CteDef.Materialization.DEFAULT
            && !capabilities.supports(SqlFeature.CTE_MATERIALIZATION)) {
            unsupported(context, node, SqlFeature.CTE_MATERIALIZATION, "with.cte");
        }
    }

    private void unsupported(
        SchemaValidationContext context,
        CteDef node,
        SqlFeature feature,
        String clausePath
    ) {
        context.addProblem(
            ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
            "PostgreSQL " + version + " does not support " + feature.description(),
            node,
            clausePath
        );
    }
}
