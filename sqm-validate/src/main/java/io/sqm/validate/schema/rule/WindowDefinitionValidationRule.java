package io.sqm.validate.schema.rule;

import io.sqm.core.Identifier;
import io.sqm.core.SelectQuery;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Validates WINDOW clause definitions in a SELECT query.
 */
final class WindowDefinitionValidationRule implements SchemaValidationRule<SelectQuery> {
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
     * Validates that each WINDOW name is unique in current SELECT scope.
     *
     * @param node select query node.
     * @param context schema validation context.
     */
    @Override
    public void validate(SelectQuery node, SchemaValidationContext context) {
        var seen = new HashSet<String>(node.windows().size());
        for (var window : node.windows()) {
            if (window.name() == null) {
                continue;
            }
            validateUniqueWindowName(window.name(), seen, context);
        }
    }

    /**
     * Records window name and reports duplicate declarations.
     *
     * @param windowName declared window name.
     * @param seen normalized names seen in current SELECT.
     * @param context schema validation context.
     */
    private static void validateUniqueWindowName(
        Identifier windowName,
        Set<String> seen,
        SchemaValidationContext context
    ) {
        var normalized = normalize(windowName);
        if (seen.add(normalized)) {
            return;
        }
        context.addProblem(
            ValidationProblem.Code.DUPLICATE_WINDOW_NAME,
            "Duplicate window name in SELECT scope: " + windowName.value(),
            "WindowDef",
            "window"
        );
    }

    /**
     * Normalizes identifier for case-insensitive comparison.
     *
     * @param identifier identifier value.
     * @return normalized identifier.
     */
    private static String normalize(Identifier identifier) {
        return identifier.value().toLowerCase(Locale.ROOT);
    }
}
