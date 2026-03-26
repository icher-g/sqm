package io.sqm.validate.postgresql.rule;

import io.sqm.core.Table;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.rule.SchemaValidationRule;

/**
 * Rejects table hints for PostgreSQL validation dialects.
 */
public final class PostgresTableHintValidationRule implements SchemaValidationRule<Table> {

    /**
     * Creates a PostgreSQL table-hint validation rule.
     */
    public PostgresTableHintValidationRule() {
    }

    @Override
    public Class<Table> nodeType() {
        return Table.class;
    }

    /**
     * Reports any table hints as unsupported for PostgreSQL.
     *
     * @param node table reference to validate.
     * @param context validation context.
     */
    @Override
    public void validate(Table node, SchemaValidationContext context) {
        if (node.hints().isEmpty()) {
            return;
        }
        context.addProblem(
            ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
            "PostgreSQL does not support table hints",
            node,
            "table.hint"
        );
    }
}
