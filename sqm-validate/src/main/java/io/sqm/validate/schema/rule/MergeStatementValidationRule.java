package io.sqm.validate.schema.rule;

import io.sqm.core.*;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Validates generic MERGE statement shape and merge-insert target columns.
 */
public final class MergeStatementValidationRule implements SchemaValidationRule<MergeStatement> {

    /**
     * Creates a merge-statement validation rule.
     */
    public MergeStatementValidationRule() {
    }

    @Override
    public Class<MergeStatement> nodeType() {
        return MergeStatement.class;
    }

    @Override
    public void validate(MergeStatement node, SchemaValidationContext context) {
        for (var clause : node.clauses()) {
            if (clause.matchType() == MergeClause.MatchType.NOT_MATCHED && clause.action() instanceof MergeInsertAction insertAction) {
                validateInsertColumns(node, insertAction, context);
            }
        }
    }

    private void validateInsertColumns(MergeStatement statement, MergeInsertAction action, SchemaValidationContext context) {
        Set<String> seenColumns = new HashSet<>();
        for (var column : action.columns()) {
            var normalized = column.value().toLowerCase(Locale.ROOT);
            if (!seenColumns.add(normalized)) {
                context.addProblem(
                    ValidationProblem.Code.COLUMN_AMBIGUOUS,
                    "Duplicate MERGE INSERT target column: " + column.value(),
                    action,
                    "merge.insert.columns"
                );
                continue;
            }
            if (context.resolvePhysicalTableColumn(statement.target(), column).isEmpty()) {
                context.addProblem(
                    ValidationProblem.Code.COLUMN_NOT_FOUND,
                    "MERGE INSERT target column not found: " + column.value(),
                    action,
                    "merge.insert.columns"
                );
            }
        }
    }
}
