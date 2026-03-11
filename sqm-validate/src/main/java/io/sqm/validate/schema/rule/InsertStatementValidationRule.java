package io.sqm.validate.schema.rule;

import io.sqm.core.Identifier;
import io.sqm.core.InsertStatement;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;

import java.util.HashSet;
import java.util.Set;

/**
 * Validates insert target columns against the physical target table schema.
 */
public final class InsertStatementValidationRule implements SchemaValidationRule<InsertStatement> {

    /**
     * Creates an insert-statement validation rule.
     */
    public InsertStatementValidationRule() {
    }

    @Override
    public Class<InsertStatement> nodeType() {
        return InsertStatement.class;
    }

    @Override
    public void validate(InsertStatement node, SchemaValidationContext context) {
        Set<String> seenColumns = new HashSet<>();
        for (Identifier column : node.columns()) {
            var normalized = column.value().toLowerCase(java.util.Locale.ROOT);
            if (!seenColumns.add(normalized)) {
                context.addProblem(
                    ValidationProblem.Code.COLUMN_AMBIGUOUS,
                    "Duplicate INSERT target column: " + column.value(),
                    node,
                    "insert.columns"
                );
                continue;
            }
            if (context.resolvePhysicalTableColumn(node.table(), column).isEmpty()) {
                context.addProblem(
                    ValidationProblem.Code.COLUMN_NOT_FOUND,
                    "Insert target column not found: " + column.value(),
                    node,
                    "insert.columns"
                );
            }
        }
    }
}
