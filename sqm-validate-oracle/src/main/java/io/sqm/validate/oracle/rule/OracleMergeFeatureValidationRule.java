package io.sqm.validate.oracle.rule;

import io.sqm.core.MergeClause;
import io.sqm.core.MergeDoNothingAction;
import io.sqm.core.MergeStatement;
import io.sqm.core.Node;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.rule.SchemaValidationRule;

import java.util.Objects;

/**
 * Validates Oracle MERGE feature usage against versioned dialect capabilities.
 */
public final class OracleMergeFeatureValidationRule implements SchemaValidationRule<MergeStatement> {
    private final DialectCapabilities capabilities;
    private final SqlDialectVersion version;

    /**
     * Creates an Oracle MERGE feature validation rule.
     *
     * @param capabilities dialect capabilities
     * @param version Oracle version
     */
    public OracleMergeFeatureValidationRule(DialectCapabilities capabilities, SqlDialectVersion version) {
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
            unsupported(context, node, "Oracle " + version + " does not support " + SqlFeature.MERGE_STATEMENT.description(), "merge");
        }
        if (node.topSpec() != null) {
            unsupported(context, node.topSpec(), "Oracle MERGE does not support TOP", "merge.top");
        }
        if (node.result() != null && !capabilities.supports(SqlFeature.MERGE_RESULT_CLAUSE)) {
            unsupported(context, node.result(), "Oracle MERGE RETURNING/OUTPUT is not supported by SQM", "merge.result");
        }
        node.clauses().stream()
            .filter(clause -> clause.matchType() == MergeClause.MatchType.NOT_MATCHED_BY_SOURCE)
            .findFirst()
            .ifPresent(clause -> unsupported(context, clause, "Oracle MERGE does not support WHEN NOT MATCHED BY SOURCE", "merge.clause"));
        node.clauses().stream()
            .filter(clause -> clause.action() instanceof MergeDoNothingAction)
            .findFirst()
            .ifPresent(clause -> unsupported(context, clause.action(), "Oracle MERGE does not support DO NOTHING actions", "merge.action"));
    }

    private void unsupported(SchemaValidationContext context, Node node, String message, String clausePath) {
        context.addProblem(
            ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
            message,
            node,
            clausePath
        );
    }
}
