package io.sqm.core;

import io.sqm.core.internal.ExprSelectItemImpl;
import io.sqm.core.internal.QualifiedStarSelectItemImpl;
import io.sqm.core.internal.StarSelectItemImpl;
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
        return new ExprSelectItemImpl(expr, null);
    }

    /**
     * Creates a '*' placeholder in a SELECT statement.
     *
     * @return {@link StarSelectItem}.
     */
    static StarSelectItem star() {
        return new StarSelectItemImpl();
    }

    /**
     * Creates a qualified '*' item for the SELECT statement.
     *
     * @param qualifier a qualifier. For example: {@code t.*}
     * @return {@link QualifiedStarSelectItem}.
     */
    static QualifiedStarSelectItem star(String qualifier) {
        return new QualifiedStarSelectItemImpl(qualifier);
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
