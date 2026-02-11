package io.sqm.validate.schema.rule;

import io.sqm.core.SelectQuery;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;

import java.util.HashSet;
import java.util.Locale;

/**
 * Validates FOR ... OF lock targets against visible sources in current SELECT scope.
 */
final class LockingClauseValidationRule implements SchemaValidationRule<SelectQuery> {
    /**
     * Normalizes SQL identifiers for case-insensitive lookup.
     *
     * @param value identifier value.
     * @return normalized key.
     */
    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    /**
     * Returns supported node type.
     *
     * @return select query type.
     */
    @Override
    public Class<SelectQuery> nodeType() {
        return SelectQuery.class;
    }

    /**
     * Validates lock targets in SELECT locking clause.
     *
     * @param node    select query node.
     * @param context schema validation context.
     */
    @Override
    public void validate(SelectQuery node, SchemaValidationContext context) {
        if (node.lockFor() == null || node.lockFor().ofTables().isEmpty()) {
            return;
        }
        var sourceKeys = new HashSet<>(context.currentScopeSourceKeys());
        for (var target : node.lockFor().ofTables()) {
            if (target == null || target.identifier() == null) {
                continue;
            }
            var normalized = normalize(target.identifier());
            if (sourceKeys.contains(normalized)) {
                continue;
            }
            context.addProblem(
                ValidationProblem.Code.LOCK_TARGET_NOT_FOUND,
                "Lock target not found in SELECT scope: " + target.identifier(),
                "LockingClause",
                "locking.of"
            );
        }
    }
}
