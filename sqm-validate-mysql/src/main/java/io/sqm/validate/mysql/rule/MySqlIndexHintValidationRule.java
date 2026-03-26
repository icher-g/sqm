package io.sqm.validate.mysql.rule;

import io.sqm.core.Table;
import io.sqm.core.TableHint;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.rule.SchemaValidationRule;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Validates common invalid MySQL index-hint combinations on a single table
 * reference.
 *
 * <p>{@code IGNORE INDEX} may coexist with either {@code USE INDEX} or
 * {@code FORCE INDEX}, but {@code USE INDEX} and {@code FORCE INDEX} conflict
 * when they target the same effective scope.</p>
 */
public final class MySqlIndexHintValidationRule implements SchemaValidationRule<Table> {
    /**
     * Creates a MySQL index-hint validation rule.
     */
    public MySqlIndexHintValidationRule() {
    }

    /**
     * Returns the node type handled by this rule.
     *
     * @return handled node type.
     */
    @Override
    public Class<Table> nodeType() {
        return Table.class;
    }

    /**
     * Validates one table reference for conflicting MySQL index-hint scopes.
     *
     * @param node table reference.
     * @param context mutable validation context.
     */
    @Override
    public void validate(Table node, SchemaValidationContext context) {
        var indexHints = node.hints().stream()
            .filter(MySqlIndexHintValidationRule::isIndexHint)
            .toList();
        if (indexHints.size() < 2) {
            return;
        }

        Map<String, EnumSet<Type>> typesByScope = new HashMap<>();

        for (var hint : indexHints) {
            for (var scope : effectiveScopes(scope(hint))) {
                typesByScope
                    .computeIfAbsent(scope, ignored -> EnumSet.noneOf(Type.class))
                    .add(type(hint));
            }
        }

        for (var entry : typesByScope.entrySet()) {
            var types = entry.getValue();
            if (types.contains(Type.USE) && types.contains(Type.FORCE)) {
                context.addProblem(
                    ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                    "MySQL does not allow USE INDEX and FORCE INDEX together for the same scope: "
                        + renderScope(entry.getKey()),
                    node,
                    "table.index_hint"
                );
                return;
            }
        }
    }

    private static Set<String> effectiveScopes(String scope) {
        if ("DEFAULT".equals(scope)) {
            return Set.of("JOIN", "ORDER BY", "GROUP BY");
        }
        return Set.of(scope);
    }

    private static String renderScope(String scope) {
        return scope;
    }

    static boolean isIndexHint(TableHint hint) {
        return hint.name().value().matches("^(USE|IGNORE|FORCE)_INDEX(_FOR_(JOIN|ORDER_BY|GROUP_BY))?$");
    }

    private static Type type(TableHint hint) {
        var name = hint.name().value();
        if (name.startsWith("USE_")) {
            return Type.USE;
        }
        if (name.startsWith("FORCE_")) {
            return Type.FORCE;
        }
        return Type.IGNORE;
    }

    private static String scope(TableHint hint) {
        return switch (hint.name().value()) {
            case "USE_INDEX_FOR_JOIN", "IGNORE_INDEX_FOR_JOIN", "FORCE_INDEX_FOR_JOIN" -> "JOIN";
            case "USE_INDEX_FOR_ORDER_BY", "IGNORE_INDEX_FOR_ORDER_BY", "FORCE_INDEX_FOR_ORDER_BY" -> "ORDER BY";
            case "USE_INDEX_FOR_GROUP_BY", "IGNORE_INDEX_FOR_GROUP_BY", "FORCE_INDEX_FOR_GROUP_BY" -> "GROUP BY";
            default -> "DEFAULT";
        };
    }

    private enum Type {
        USE,
        IGNORE,
        FORCE
    }
}
