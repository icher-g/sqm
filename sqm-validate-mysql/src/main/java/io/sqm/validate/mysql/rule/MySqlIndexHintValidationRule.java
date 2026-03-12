package io.sqm.validate.mysql.rule;

import io.sqm.core.Table;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.rule.SchemaValidationRule;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

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
        if (node.indexHints().size() < 2) {
            return;
        }

        Map<Table.IndexHintScope, EnumSet<Table.IndexHintType>> typesByScope = new EnumMap<>(Table.IndexHintScope.class);

        for (var hint : node.indexHints()) {
            for (var scope : effectiveScopes(hint.scope())) {
                typesByScope
                    .computeIfAbsent(scope, ignored -> EnumSet.noneOf(Table.IndexHintType.class))
                    .add(hint.type());
            }
        }

        for (var entry : typesByScope.entrySet()) {
            var types = entry.getValue();
            if (types.contains(Table.IndexHintType.USE) && types.contains(Table.IndexHintType.FORCE)) {
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

    private static EnumSet<Table.IndexHintScope> effectiveScopes(Table.IndexHintScope scope) {
        if (scope == Table.IndexHintScope.DEFAULT) {
            return EnumSet.of(
                Table.IndexHintScope.JOIN,
                Table.IndexHintScope.ORDER_BY,
                Table.IndexHintScope.GROUP_BY
            );
        }
        return EnumSet.of(scope);
    }

    private static String renderScope(Table.IndexHintScope scope) {
        return switch (scope) {
            case JOIN -> "JOIN";
            case ORDER_BY -> "ORDER BY";
            case GROUP_BY -> "GROUP BY";
            case DEFAULT -> "DEFAULT";
        };
    }
}
