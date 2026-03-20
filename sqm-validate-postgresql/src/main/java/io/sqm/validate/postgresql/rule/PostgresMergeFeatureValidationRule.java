package io.sqm.validate.postgresql.rule;

import io.sqm.core.MergeStatement;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.rule.SchemaValidationRule;

import java.util.Objects;

/**
 * Validates PostgreSQL MERGE feature availability by server version.
 */
public final class PostgresMergeFeatureValidationRule implements SchemaValidationRule<MergeStatement> {
    private final DialectCapabilities capabilities;
    private final SqlDialectVersion version;

    /**
     * Creates a PostgreSQL MERGE feature validation rule.
     *
     * @param capabilities dialect capabilities
     * @param version PostgreSQL version
     */
    public PostgresMergeFeatureValidationRule(DialectCapabilities capabilities, SqlDialectVersion version) {
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
        this.version = Objects.requireNonNull(version, "version");
    }

    @Override
    public Class<MergeStatement> nodeType() {
        return MergeStatement.class;
    }

    @Override
    public void validate(MergeStatement node, SchemaValidationContext context) {
        if (capabilities.supports(SqlFeature.MERGE_STATEMENT)) {
            return;
        }
        context.addProblem(
            ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
            "PostgreSQL " + version + " does not support " + SqlFeature.MERGE_STATEMENT.description(),
            node,
            "merge"
        );
    }
}
