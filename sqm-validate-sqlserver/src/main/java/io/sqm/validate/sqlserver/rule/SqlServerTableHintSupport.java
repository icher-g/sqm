package io.sqm.validate.sqlserver.rule;

import io.sqm.core.ResultInto;
import io.sqm.core.Table;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;

import java.util.EnumSet;

/**
 * Shared validation helpers for SQL Server table hint semantics.
 */
final class SqlServerTableHintSupport {
    private SqlServerTableHintSupport() {
    }

    static void validateHints(Table table, SchemaValidationContext context, String clausePath) {
        if (table.lockHints().isEmpty()) {
            return;
        }

        var seen = EnumSet.noneOf(Table.LockHintKind.class);
        for (var hint : table.lockHints()) {
            if (!seen.add(hint.kind())) {
                context.addProblem(
                    ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                    "Duplicate SQL Server table hint " + hint.kind().name(),
                    table,
                    clausePath
                );
            }
        }

        if (seen.contains(Table.LockHintKind.NOLOCK)
            && (seen.contains(Table.LockHintKind.UPDLOCK) || seen.contains(Table.LockHintKind.HOLDLOCK))) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                "SQL Server NOLOCK cannot be combined with UPDLOCK or HOLDLOCK",
                table,
                clausePath
            );
        }
    }

    static void validateResultIntoTarget(ResultInto into, SchemaValidationContext context, String clausePath) {
        if (into == null || into.target().lockHints().isEmpty()) {
            return;
        }
        context.addProblem(
            ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
            "SQL Server table hints are not supported on OUTPUT INTO targets",
            into.target(),
            clausePath
        );
    }
}
