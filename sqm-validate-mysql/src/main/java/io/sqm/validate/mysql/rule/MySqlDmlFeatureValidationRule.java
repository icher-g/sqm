package io.sqm.validate.mysql.rule;

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
 * Validates MySQL DML feature usage against versioned dialect capabilities.
 */
public final class MySqlDmlFeatureValidationRule implements SchemaValidationRule<Statement> {
    private final DialectCapabilities capabilities;
    private final SqlDialectVersion version;

    /**
     * Creates a MySQL DML feature validation rule.
     *
     * @param capabilities dialect capabilities
     * @param version MySQL version
     */
    public MySqlDmlFeatureValidationRule(DialectCapabilities capabilities, SqlDialectVersion version) {
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
        this.version = Objects.requireNonNull(version, "version");
    }

    @Override
    public Class<Statement> nodeType() {
        return Statement.class;
    }

    /**
     * Validates MySQL-specific DML statement features.
     *
     * @param node statement to validate
     * @param context validation context
     */
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
            case IGNORE -> require(context, node, SqlFeature.INSERT_IGNORE, "insert.mode");
            case REPLACE -> require(context, node, SqlFeature.REPLACE_INTO, "insert.mode");
            case STANDARD -> {
                // no-op
            }
        }

        if (node.onConflictAction() != InsertStatement.OnConflictAction.NONE) {
            require(context, node, SqlFeature.INSERT_ON_DUPLICATE_KEY_UPDATE, "insert.conflict");
        }

        if (node.result() != null && !node.result().items().isEmpty()) {
            require(context, node.result(), SqlFeature.DML_RESULT_CLAUSE, "insert.result");
        }
    }

    private void validateUpdate(UpdateStatement node, SchemaValidationContext context) {
        if (!node.joins().isEmpty()) {
            require(context, node, SqlFeature.UPDATE_JOIN, "update.join");
        }

        if (node.result() != null && !node.result().items().isEmpty()) {
            require(context, node.result(), SqlFeature.DML_RESULT_CLAUSE, "update.result");
        }
    }

    private void validateDelete(DeleteStatement node, SchemaValidationContext context) {
        if (!node.using().isEmpty() || !node.joins().isEmpty()) {
            require(context, node, SqlFeature.DELETE_USING_JOIN, "delete.using");
        }

        if (node.result() != null && !node.result().items().isEmpty()) {
            require(context, node.result(), SqlFeature.DML_RESULT_CLAUSE, "delete.result");
        }
    }

    private void require(
        SchemaValidationContext context,
        Node node,
        SqlFeature feature,
        String clausePath
    ) {
        if (capabilities.supports(feature)) {
            return;
        }
        context.addProblem(
            ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
            "MySQL " + version + " does not support " + feature.description(),
            node,
            clausePath
        );
    }
}