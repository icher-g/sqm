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
        if (!capabilities.supports(SqlFeature.MERGE_STATEMENT)) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
                "PostgreSQL " + version + " does not support " + SqlFeature.MERGE_STATEMENT.description(),
                node,
                "merge"
            );
        }
        if (node.topSpec() != null) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
                "PostgreSQL MERGE does not support TOP",
                node.topSpec(),
                "merge.top"
            );
        }
        if (node.result() != null && !node.result().items().isEmpty() && !capabilities.supports(SqlFeature.MERGE_RESULT_CLAUSE)) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
                "PostgreSQL " + version + " does not support MERGE RETURNING",
                node.result(),
                "merge.result"
            );
        }
        node.clauses().stream()
            .filter(clause -> clause.matchType() == io.sqm.core.MergeClause.MatchType.NOT_MATCHED_BY_SOURCE)
            .filter(clause -> !capabilities.supports(SqlFeature.MERGE_NOT_MATCHED_BY_SOURCE_CLAUSE))
            .findFirst()
            .ifPresent(clause -> context.addProblem(
                ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
                "PostgreSQL " + version + " does not support MERGE WHEN NOT MATCHED BY SOURCE clauses",
                clause,
                "merge.clause"
            ));
    }
}
