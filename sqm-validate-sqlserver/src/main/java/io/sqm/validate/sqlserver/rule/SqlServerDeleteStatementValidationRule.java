package io.sqm.validate.sqlserver.rule;

import io.sqm.core.DeleteStatement;
import io.sqm.core.OutputColumnExpr;
import io.sqm.core.OutputRowSource;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.rule.SchemaValidationRule;

/**
 * Validates SQL Server baseline DELETE support.
 */
public final class SqlServerDeleteStatementValidationRule implements SchemaValidationRule<DeleteStatement> {

    /**
     * Creates a SQL Server delete validation rule.
     */
    public SqlServerDeleteStatementValidationRule() {
    }

    @Override
    public Class<DeleteStatement> nodeType() {
        return DeleteStatement.class;
    }

    /**
     * Validates SQL Server unsupported DELETE extensions.
     *
     * @param node delete statement.
     * @param context validation context.
     */
    @Override
    public void validate(DeleteStatement node, SchemaValidationContext context) {
        if (!node.using().isEmpty()) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
                "SQL Server baseline support does not include DELETE ... USING",
                node,
                "delete.using"
            );
        }
        if (!node.joins().isEmpty()) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
                "SQL Server baseline support does not include DELETE ... JOIN",
                node,
                "delete.join"
            );
        }
        if (!node.returning().isEmpty()) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
                "SQL Server baseline support does not include DELETE RETURNING",
                node,
                "delete.returning"
            );
        }
        if (!node.optimizerHints().isEmpty()) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
                "SQL Server baseline support does not include optimizer hint comments on DELETE",
                node,
                "delete.hint"
            );
        }
        if (node.output() != null) {
            for (var item : node.output().items()) {
                if (item.expression() instanceof OutputColumnExpr outputColumn && outputColumn.source() == OutputRowSource.INSERTED) {
                    context.addProblem(
                        ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                        "DELETE OUTPUT may reference deleted.<column> values, but not inserted.<column>",
                        item,
                        "delete.output"
                    );
                }
            }
        }
    }
}
