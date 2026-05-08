package io.sqm.validate.sqlserver.rule;

import io.sqm.core.RelationResultTarget;
import io.sqm.core.ResultClause;
import io.sqm.core.Table;
import io.sqm.core.VariableTableRef;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;

import java.util.HashSet;

/**
 * Shared validation helpers for SQL Server table hint semantics.
 */
final class SqlServerTableHintSupport {
    private SqlServerTableHintSupport() {
    }

    static void validateHints(Table table, SchemaValidationContext context, String clausePath) {
        if (table.hints().isEmpty()) {
            return;
        }

        var hints = table.hints().stream()
            .filter(SqlServerTableHintSupport::isLockHint)
            .toList();

        if (hints.size() != table.hints().size()) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
                "SQL Server baseline support currently includes only lock-style table hints",
                table,
                clausePath
            );
            return;
        }

        var seen = new HashSet<String>();
        for (var hint : hints) {
            if (!seen.add(hint.name().value())) {
                context.addProblem(
                    ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                    "Duplicate SQL Server table hint " + hint.name().value(),
                    table,
                    clausePath
                );
            }
        }

        if (seen.contains("NOLOCK") && (seen.contains("UPDLOCK") || seen.contains("HOLDLOCK"))) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                "SQL Server NOLOCK cannot be combined with UPDLOCK or HOLDLOCK",
                table,
                clausePath
            );
        }
    }

    static void validateRelationResultTargetTarget(RelationResultTarget into, SchemaValidationContext context, String clausePath) {
        if (into == null) {
            return;
        }
        if (into.target() instanceof VariableTableRef) {
            return;
        }
        if (!(into.target() instanceof Table targetTable)) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
                "SQL Server OUTPUT INTO currently supports base tables and table variables only",
                into.target(),
                clausePath
            );
            return;
        }
        if (targetTable.hints().stream().noneMatch(SqlServerTableHintSupport::isLockHint)) {
            return;
        }
        context.addProblem(
            ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
            "SQL Server table hints are not supported on OUTPUT INTO targets",
            targetTable,
            clausePath
        );
    }

    static void validateResultTarget(ResultClause result, SchemaValidationContext context, String clausePath) {
        if (result == null || result.target() == null) {
            return;
        }
        if (result.target() instanceof RelationResultTarget target) {
            validateRelationResultTargetTarget(target, context, clausePath);
            return;
        }
        context.addProblem(
            ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
            "SQL Server OUTPUT supports only relation targets in OUTPUT INTO",
            result.target(),
            clausePath
        );
    }

    private static boolean isLockHint(io.sqm.core.TableHint hint) {
        return switch (hint.name().value()) {
            case "NOLOCK", "UPDLOCK", "HOLDLOCK" -> true;
            default -> false;
        };
    }
}
