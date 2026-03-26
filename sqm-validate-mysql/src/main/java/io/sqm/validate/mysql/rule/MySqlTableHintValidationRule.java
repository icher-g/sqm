package io.sqm.validate.mysql.rule;

import io.sqm.core.IdentifierHintArg;
import io.sqm.core.Table;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.rule.SchemaValidationRule;

/**
 * Validates supported MySQL table-hint families and argument shapes.
 */
public final class MySqlTableHintValidationRule implements SchemaValidationRule<Table> {

    /**
     * Creates a MySQL table-hint validation rule.
     */
    public MySqlTableHintValidationRule() {
    }

    @Override
    public Class<Table> nodeType() {
        return Table.class;
    }

    /**
     * Validates MySQL table-hint family support and basic index-hint argument structure.
     *
     * @param node table reference to validate.
     * @param context validation context.
     */
    @Override
    public void validate(Table node, SchemaValidationContext context) {
        for (var hint : node.hints()) {
            if (!MySqlIndexHintValidationRule.isIndexHint(hint)) {
                context.addProblem(
                    ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
                    "MySQL first-wave validation does not support table hint " + hint.name().value(),
                    hint,
                    "table.hint"
                );
                continue;
            }

            if (hint.args().isEmpty()) {
                context.addProblem(
                    ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                    "MySQL index hints require at least one index identifier",
                    hint,
                    "table.hint"
                );
            }

            for (var arg : hint.args()) {
                if (!(arg instanceof IdentifierHintArg)) {
                    context.addProblem(
                        ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                        "MySQL index hints require identifier arguments",
                        hint,
                        "table.hint"
                    );
                    break;
                }
            }
        }
    }
}
