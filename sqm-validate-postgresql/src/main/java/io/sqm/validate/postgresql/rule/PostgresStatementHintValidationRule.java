package io.sqm.validate.postgresql.rule;

import io.sqm.core.DeleteStatement;
import io.sqm.core.InsertStatement;
import io.sqm.core.MergeStatement;
import io.sqm.core.SelectQuery;
import io.sqm.core.Statement;
import io.sqm.core.UpdateStatement;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.rule.SchemaValidationRule;

/**
 * Rejects statement hints for PostgreSQL validation dialects.
 */
public final class PostgresStatementHintValidationRule implements SchemaValidationRule<Statement> {

    /**
     * Creates a PostgreSQL statement-hint validation rule.
     */
    public PostgresStatementHintValidationRule() {
    }

    @Override
    public Class<Statement> nodeType() {
        return Statement.class;
    }

    /**
     * Reports any statement hints as unsupported for PostgreSQL.
     *
     * @param node statement to validate.
     * @param context validation context.
     */
    @Override
    public void validate(Statement node, SchemaValidationContext context) {
        if (node.hints().isEmpty()) {
            return;
        }
        context.addProblem(
            ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
            "PostgreSQL does not support native statement hints",
            node,
            clausePath(node)
        );
    }

    private static String clausePath(Statement statement) {
        if (statement instanceof SelectQuery) {
            return "select.hint";
        }
        if (statement instanceof InsertStatement) {
            return "insert.hint";
        }
        if (statement instanceof UpdateStatement) {
            return "update.hint";
        }
        if (statement instanceof DeleteStatement) {
            return "delete.hint";
        }
        if (statement instanceof MergeStatement) {
            return "merge.hint";
        }
        return "statement.hint";
    }
}
