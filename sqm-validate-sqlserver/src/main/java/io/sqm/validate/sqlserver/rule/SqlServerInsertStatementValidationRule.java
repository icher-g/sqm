package io.sqm.validate.sqlserver.rule;

import io.sqm.core.InsertStatement;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.rule.SchemaValidationRule;

/**
 * Validates SQL Server baseline INSERT support.
 */
public final class SqlServerInsertStatementValidationRule implements SchemaValidationRule<InsertStatement> {

    /**
     * Creates a SQL Server insert validation rule.
     */
    public SqlServerInsertStatementValidationRule() {
    }

    @Override
    public Class<InsertStatement> nodeType() {
        return InsertStatement.class;
    }

    /**
     * Validates SQL Server unsupported INSERT extensions.
     *
     * @param node insert statement.
     * @param context validation context.
     */
    @Override
    public void validate(InsertStatement node, SchemaValidationContext context) {
        if (node.insertMode() != InsertStatement.InsertMode.STANDARD) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
                "SQL Server baseline support only allows standard INSERT INTO",
                node,
                "insert.mode"
            );
        }
        if (node.onConflictAction() != InsertStatement.OnConflictAction.NONE) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
                "SQL Server does not support INSERT ... ON CONFLICT",
                node,
                "insert.on_conflict"
            );
        }
        if (!node.returning().isEmpty()) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
                "SQL Server baseline support does not include INSERT RETURNING",
                node,
                "insert.returning"
            );
        }
    }
}
