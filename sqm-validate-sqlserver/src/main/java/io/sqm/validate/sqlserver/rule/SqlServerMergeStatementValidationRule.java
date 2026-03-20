package io.sqm.validate.sqlserver.rule;

import io.sqm.core.MergeClause;
import io.sqm.core.MergeDeleteAction;
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

        if (node.result() != null) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
                "SQL Server MERGE OUTPUT is not supported yet",
                node,
                "merge.result"
            );
        }

        boolean matchedUpdate = false;
        boolean matchedDelete = false;
        boolean notMatchedInsert = false;

        for (var clause : node.clauses()) {
            if (clause.matchType() == MergeClause.MatchType.MATCHED && clause.action() instanceof MergeUpdateAction) {
                if (matchedUpdate) {
                    context.addProblem(
                        ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                        "SQL Server MERGE supports at most one WHEN MATCHED THEN UPDATE clause in this slice",
                        clause,
                        "merge.clause"
                    );
                }
                matchedUpdate = true;
            }
            else if (clause.matchType() == MergeClause.MatchType.MATCHED && clause.action() instanceof MergeDeleteAction) {
                if (matchedDelete) {
                    context.addProblem(
                        ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                        "SQL Server MERGE supports at most one WHEN MATCHED THEN DELETE clause in this slice",
                        clause,
                        "merge.clause"
                    );
                }
                matchedDelete = true;
            }
            else if (clause.matchType() == MergeClause.MatchType.NOT_MATCHED && clause.action() instanceof MergeInsertAction) {
                if (notMatchedInsert) {
                    context.addProblem(
                        ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                        "SQL Server MERGE supports at most one WHEN NOT MATCHED THEN INSERT clause in this slice",
                        clause,
                        "merge.clause"
                    );
                }
                notMatchedInsert = true;
            }
        }
    }
}
