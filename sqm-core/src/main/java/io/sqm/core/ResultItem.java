package io.sqm.core;

import io.sqm.core.match.ResultItemMatch;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single item inside a {@link ResultClause}.
 * <p>
 * Structurally this is similar to a select expression with an optional alias, but it is modeled
 * separately because it belongs to mutation-result semantics rather than to a {@code SELECT} list.
 * <p>
 * Examples:
 * <ul>
 *     <li>{@code RETURNING id}</li>
 *     <li>{@code RETURNING now() AS ts}</li>
 *     <li>{@code OUTPUT INSERTED.id AS new_id}</li>
 *     <li>{@code OUTPUT INSERTED.*}</li>
 * </ul>
 */
public sealed interface ResultItem extends Node permits ExprResultItem, OutputStarResultItem, QualifiedStarResultItem, StarResultItem {

    /**
     * Creates an expression wrapper for a SELECT statement.
     *
     * @param expr an expression to wrap.
     * @return A newly created instance of a wrapper.
     */
    static ExprResultItem expr(Expression expr) {
        return ExprResultItem.of(expr);
    }

    /**
     * Creates a '*' placeholder in a SELECT statement.
     *
     * @return {@link StarResultItem}.
     */
    static StarResultItem star() {
        return StarResultItem.of();
    }

    /**
     * Creates a qualified star select item.
     *
     * @param qualifierIdentifier a qualifier identifier before '*'
     * @return a qualified star select item.
     */
    static QualifiedStarResultItem star(Identifier qualifierIdentifier) {
        return QualifiedStarResultItem.of(qualifierIdentifier);
    }

    /**
     * Creates an {@code inserted.*} SQL Server output item.
     *
     * @return output-star result item
     */
    static OutputStarResultItem insertedStar() {
        return OutputStarResultItem.inserted();
    }

    /**
     * Creates a {@code deleted.*} SQL Server output item.
     *
     * @return output-star result item
     */
    static OutputStarResultItem deletedStar() {
        return OutputStarResultItem.deleted();
    }

    /**
     * Creates {@link ResultItem} from {@link Node} if possible.
     *
     * @param nodes a list of nodes to convert to {@link ResultItem}l
     * @return a list of {@link ResultItem}'s.
     */
    static List<ResultItem> fromNodes(Node... nodes) {
        var items = new ArrayList<ResultItem>();
        for (var expr : nodes) {
            switch (expr) {
                case Expression expression -> items.add(ResultItem.expr(expression));
                case ResultItem resultItem -> items.add(resultItem);
                case ExprSelectItem exprSelectItem -> items.add(ExprResultItem.of(exprSelectItem.expr(), exprSelectItem.alias()));
                case StarSelectItem ignore -> items.add(StarResultItem.of());
                case QualifiedStarSelectItem qStar -> items.add(QualifiedStarResultItem.of(qStar.qualifier()));
                default -> throw new IllegalStateException("The provided node is not supported in the result clause: " + expr);
            }
        }
        return List.copyOf(items);
    }

    /**
     * Creates a new matcher for the current {@link ResultItem}.
     *
     * @param <R> the result type
     * @return a new {@code ResultItemMatch}.
     */
    default <R> ResultItemMatch<R> matchResultItem() {
        return ResultItemMatch.match(this);
    }
}
