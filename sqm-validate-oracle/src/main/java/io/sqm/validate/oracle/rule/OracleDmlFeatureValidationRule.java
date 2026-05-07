package io.sqm.validate.oracle.rule;

import io.sqm.core.DeleteStatement;
import io.sqm.core.InsertStatement;
import io.sqm.core.Node;
import io.sqm.core.Statement;
import io.sqm.core.UpdateStatement;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.rule.SchemaValidationRule;

import java.util.Objects;

/**
 * Validates Oracle DML feature usage against versioned dialect capabilities.
 */
public final class OracleDmlFeatureValidationRule implements SchemaValidationRule<Statement> {
    private final DialectCapabilities capabilities;
    private final SqlDialectVersion version;

    /**
     * Creates an Oracle DML feature validation rule.
     *
     * @param capabilities dialect capabilities
     * @param version Oracle version
     */
    public OracleDmlFeatureValidationRule(DialectCapabilities capabilities, SqlDialectVersion version) {
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
        this.version = Objects.requireNonNull(version, "version");
    }

    @Override
    public Class<Statement> nodeType() {
        return Statement.class;
    }

    @Override
    public void validate(Statement node, SchemaValidationContext context) {
        if (node instanceof InsertStatement insert) {
            validateInsert(insert, context);
            return;
        }
        if (node instanceof UpdateStatement update) {
            validateUpdate(update, context);
            return;
        }
        if (node instanceof DeleteStatement delete) {
            validateDelete(delete, context);
        }
    }

    private void validateInsert(InsertStatement node, SchemaValidationContext context) {
        switch (node.insertMode()) {
            case IGNORE -> unsupported(context, node, "Oracle does not support INSERT IGNORE", "insert.mode");
            case REPLACE -> unsupported(context, node, "Oracle does not support REPLACE INTO", "insert.mode");
            case STANDARD -> {
                // no-op
            }
        }

        if (node.onConflictAction() != InsertStatement.OnConflictAction.NONE) {
            unsupported(context, node, "Oracle does not support ON CONFLICT", "insert.conflict");
        }
        rejectResult(node.result(), context, "insert.result");
    }

    private void validateUpdate(UpdateStatement node, SchemaValidationContext context) {
        if (!node.joins().isEmpty()) {
            unsupported(context, node, "Oracle does not support UPDATE JOIN", "update.join");
        }
        if (!node.from().isEmpty()) {
            unsupported(context, node, "Oracle does not support UPDATE FROM", "update.from");
        }
        rejectResult(node.result(), context, "update.result");
    }

    private void validateDelete(DeleteStatement node, SchemaValidationContext context) {
        if (!node.using().isEmpty() || !node.joins().isEmpty()) {
            unsupported(context, node, "Oracle does not support DELETE USING/JOIN", "delete.using");
        }
        rejectResult(node.result(), context, "delete.result");
    }

    private void rejectResult(Node result, SchemaValidationContext context, String clausePath) {
        if (result == null || capabilities.supports(SqlFeature.DML_RESULT_CLAUSE)) {
            return;
        }
        unsupported(
            context,
            result,
            "Oracle " + version + " RETURNING INTO is not supported by SQM DML result clauses",
            clausePath
        );
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
