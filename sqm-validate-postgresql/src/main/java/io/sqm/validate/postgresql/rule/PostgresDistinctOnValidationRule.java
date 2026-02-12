package io.sqm.validate.postgresql.rule;

import io.sqm.core.ExprSelectItem;
import io.sqm.core.Expression;
import io.sqm.core.SelectQuery;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.rule.SchemaValidationRule;

import java.util.Optional;

/**
 * Validates PostgreSQL DISTINCT ON alignment with leftmost ORDER BY expressions.
 */
public final class PostgresDistinctOnValidationRule implements SchemaValidationRule<SelectQuery> {
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
     * Validates DISTINCT ON ordering semantics.
     *
     * @param node select query node.
     * @param context schema validation context.
     */
    @Override
    public void validate(SelectQuery node, SchemaValidationContext context) {
        if (node.distinct() == null || node.distinct().items().isEmpty()) {
            return;
        }

        var distinctItems = node.distinct().items();
        if (node.orderBy() == null || node.orderBy().items().size() < distinctItems.size()) {
            context.addProblem(
                ValidationProblem.Code.DISTINCT_ON_ORDER_BY_MISMATCH,
                "DISTINCT ON requires ORDER BY with matching leftmost expressions",
                node,
                "distinct"
            );
            return;
        }

        for (int i = 0; i < distinctItems.size(); i++) {
            var expected = distinctItems.get(i);
            var actual = resolveOrderExpression(node, i);
            if (actual.isEmpty() || !expected.equals(actual.get())) {
                context.addProblem(
                    ValidationProblem.Code.DISTINCT_ON_ORDER_BY_MISMATCH,
                    "DISTINCT ON expression at position "
                        + (i + 1)
                        + " must match ORDER BY expression at the same position",
                    node,
                    "distinct"
                );
                return;
            }
        }
    }

    /**
     * Resolves ORDER BY expression for one position.
     *
     * <p>Supports direct ORDER BY expressions and ORDER BY ordinals that point
     * to expression select items.</p>
     *
     * @param query owner select query.
     * @param index zero-based ORDER BY index.
     * @return resolved expression if available.
     */
    private static Optional<Expression> resolveOrderExpression(SelectQuery query, int index) {
        var item = query.orderBy().items().get(index);
        if (item.expr() != null) {
            return Optional.of(item.expr());
        }
        if (item.ordinal() == null) {
            return Optional.empty();
        }
        var selectIndex = item.ordinal() - 1;
        if (selectIndex < 0 || selectIndex >= query.items().size()) {
            return Optional.empty();
        }
        var selectItem = query.items().get(selectIndex);
        if (selectItem instanceof ExprSelectItem exprSelectItem) {
            return Optional.of(exprSelectItem.expr());
        }
        return Optional.empty();
    }
}
