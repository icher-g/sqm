package io.sqm.core;

import io.sqm.core.match.SelectItemMatch;

/**
 * A single item in the SELECT list.
 */
public sealed interface SelectItem extends Node
    permits DialectSelectItem, ExprSelectItem, QualifiedStarSelectItem, StarSelectItem {

    /**
     * Creates an expression wrapper for a SELECT statement.
     *
     * @param expr an expression to wrap.
     * @return A newly created instance of a wrapper.
     */
    static ExprSelectItem expr(Expression expr) {
        return ExprSelectItem.of(expr);
    }

    /**
     * Creates a '*' placeholder in a SELECT statement.
     *
     * @return {@link StarSelectItem}.
     */
    static StarSelectItem star() {
        return StarSelectItem.of();
    }

    /**
     * Creates a qualified star select item.
     *
     * @param qualifierIdentifier a qualifier identifier before '*'
     * @return a qualified star select item.
     */
    static QualifiedStarSelectItem star(Identifier qualifierIdentifier) {
        return QualifiedStarSelectItem.of(qualifierIdentifier);
    }

    /**
     * Creates a new matcher for the current {@link SelectItem}.
     *
     * @param <R> the result type
     * @return a new {@code SelectItemMatch}.
     */
    default <R> SelectItemMatch<R> matchSelectItem() {
        return SelectItemMatch.match(this);
    }
}
