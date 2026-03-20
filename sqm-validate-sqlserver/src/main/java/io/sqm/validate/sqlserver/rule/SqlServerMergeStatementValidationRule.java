package io.sqm.validate.sqlserver.rule;

import io.sqm.core.MergeClause;
import io.sqm.core.MergeDeleteAction;
import io.sqm.core.MergeDoNothingAction;
import io.sqm.core.MergeInsertAction;
import io.sqm.core.MergeStatement;
import io.sqm.core.MergeUpdateAction;
import io.sqm.core.Table;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.rule.SchemaValidationRule;

/**
 * Validates the first SQL Server MERGE implementation slice.
 */
public final class SqlServerMergeStatementValidationRule implements SchemaValidationRule<MergeStatement> {

    /**
     * Creates a SQL Server merge-statement validation rule.
     */
    public SqlServerMergeStatementValidationRule() {
    }

    @Override
    public Class<MergeStatement> nodeType() {
        return MergeStatement.class;
    }

    @Override
    public void validate(MergeStatement node, SchemaValidationContext context) {
        SqlServerTableHintSupport.validateHints(node.target(), context, "merge.target");
        if (node.source() instanceof Table sourceTable) {
            SqlServerTableHintSupport.validateHints(sourceTable, context, "merge.source");
        }
        if (node.topSpec() != null && node.topSpec().withTies()) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                "SQL Server MERGE TOP does not support WITH TIES",
                node.topSpec(),
                "merge.top"
            );
        }
        SqlServerTableHintSupport.validateResultIntoTarget(
            node.result() == null ? null : node.result().into(),
            context,
            "merge.result"
        );

        if (node.clauses().stream().anyMatch(clause -> clause.action() instanceof MergeDoNothingAction)) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
                "SQL Server MERGE DO NOTHING actions are not supported",
                node,
                "merge.clause"
            );
        }

        var matchedClauses = node.clauses().stream()
            .filter(clause -> clause.matchType() == MergeClause.MatchType.MATCHED)
            .toList();
        if (matchedClauses.size() > 2) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                "SQL Server MERGE supports at most two WHEN MATCHED clauses",
                node,
                "merge.clause"
            );
        }

        if (matchedClauses.size() == 2) {
            validateDualUpdateDeleteClauses(matchedClauses, "WHEN MATCHED", node, context);
        }

        long notMatchedInsertCount = node.clauses().stream()
            .filter(clause -> clause.matchType() == MergeClause.MatchType.NOT_MATCHED && clause.action() instanceof MergeInsertAction)
            .count();

        if (notMatchedInsertCount > 1) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                "SQL Server MERGE supports at most one WHEN NOT MATCHED THEN INSERT clause",
                node,
                "merge.clause"
            );
        }

        var notMatchedBySourceClauses = node.clauses().stream()
            .filter(clause -> clause.matchType() == MergeClause.MatchType.NOT_MATCHED_BY_SOURCE)
            .toList();

        if (notMatchedBySourceClauses.size() > 2) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                "SQL Server MERGE supports at most two WHEN NOT MATCHED BY SOURCE clauses",
                node,
                "merge.clause"
            );
        }

        if (notMatchedBySourceClauses.size() == 2) {
            validateDualUpdateDeleteClauses(notMatchedBySourceClauses, "WHEN NOT MATCHED BY SOURCE", node, context);
        }
    }

    private void validateDualUpdateDeleteClauses(
        java.util.List<MergeClause> clauses,
        String label,
        MergeStatement node,
        SchemaValidationContext context
    ) {
        if (clauses.getFirst().condition() == null) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                "SQL Server MERGE requires the first " + label + " clause to include AND <search_condition> when two "
                    + label + " clauses are present",
                clauses.getFirst(),
                "merge.clause"
            );
        }

        var firstAction = clauses.getFirst().action();
        var secondAction = clauses.get(1).action();

        if ((firstAction instanceof MergeUpdateAction && secondAction instanceof MergeUpdateAction)
            || (firstAction instanceof MergeDeleteAction && secondAction instanceof MergeDeleteAction)) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                "SQL Server MERGE requires one UPDATE and one DELETE action when two " + label + " clauses are present",
                node,
                "merge.clause"
            );
        }
    }
}
