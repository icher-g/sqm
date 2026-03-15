package io.sqm.validate.sqlserver.rule;

import io.sqm.core.UpdateStatement;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.rule.SchemaValidationRule;

/**
 * Validates SQL Server baseline UPDATE support.
 */
public final class SqlServerUpdateStatementValidationRule implements SchemaValidationRule<UpdateStatement> {

    /**
     * Creates a SQL Server update validation rule.
     */
    public SqlServerUpdateStatementValidationRule() {
    }

    @Override
    public Class<UpdateStatement> nodeType() {
        return UpdateStatement.class;
    }

    /**
     * Validates SQL Server unsupported UPDATE extensions.
     *
     * @param node update statement.
     * @param context validation context.
     */
    @Override
    public void validate(UpdateStatement node, SchemaValidationContext context) {
        if (!node.joins().isEmpty()) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
                "SQL Server baseline support does not include UPDATE ... JOIN",
                node,
                "update.join"
            );
        }
        if (!node.from().isEmpty()) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
                "SQL Server baseline support does not include UPDATE ... FROM",
                node,
                "update.from"
            );
        }
        if (!node.returning().isEmpty()) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
                "SQL Server baseline support does not include UPDATE RETURNING",
                node,
                "update.returning"
            );
        }
        if (!node.optimizerHints().isEmpty()) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
                "SQL Server baseline support does not include optimizer hint comments on UPDATE",
                node,
                "update.hint"
            );
        }
    }
}
