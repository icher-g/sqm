package io.sqm.validate.sqlserver.rule;

import io.sqm.core.PivotTable;
import io.sqm.core.TableRef;
import io.sqm.core.UnpivotTable;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.rule.SchemaValidationRule;

/**
 * Validates SQL Server-specific {@code PIVOT} and {@code UNPIVOT} shape restrictions.
 */
public final class SqlServerPivotValidationRule implements SchemaValidationRule<TableRef> {
    /**
     * Creates a SQL Server pivot validation rule.
     */
    public SqlServerPivotValidationRule() {
    }

    /**
     * Returns the node type this rule validates.
     *
     * @return table reference class
     */
    @Override
    public Class<TableRef> nodeType() {
        return TableRef.class;
    }

    /**
     * Validates SQL Server pivot and unpivot restrictions.
     *
     * @param node table reference
     * @param context validation context
     */
    @Override
    public void validate(TableRef node, SchemaValidationContext context) {
        if (node instanceof PivotTable pivot) {
            validatePivot(pivot, context);
        }
        if (node instanceof UnpivotTable unpivot) {
            validateUnpivot(unpivot, context);
        }
    }

    private static void validatePivot(PivotTable pivot, SchemaValidationContext context) {
        if (pivot.measures().stream().anyMatch(measure -> measure.alias() != null)) {
            invalid(context, pivot, "SQL Server PIVOT does not support measure aliases", "from.pivot");
        }
        if (pivot.values().stream().anyMatch(value -> value.alias() != null)) {
            invalid(context, pivot, "SQL Server PIVOT does not support value aliases", "from.pivot");
        }
    }

    private static void validateUnpivot(UnpivotTable unpivot, SchemaValidationContext context) {
        if (unpivot.nullTreatment() != UnpivotTable.NullTreatment.DIALECT_DEFAULT) {
            invalid(context, unpivot, "SQL Server UNPIVOT does not support INCLUDE NULLS or EXCLUDE NULLS", "from.unpivot");
        }
    }

    private static void invalid(SchemaValidationContext context, TableRef node, String message, String path) {
        context.addProblem(ValidationProblem.Code.DIALECT_CLAUSE_INVALID, message, node, path);
    }
}
