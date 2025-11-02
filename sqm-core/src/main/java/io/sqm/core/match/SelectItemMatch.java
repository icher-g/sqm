package io.sqm.core.match;

import io.sqm.core.ExprSelectItem;
import io.sqm.core.QualifiedStarSelectItem;
import io.sqm.core.SelectItem;
import io.sqm.core.StarSelectItem;

import java.util.function.Function;

/**
 * Pattern-style matcher for {@link SelectItem} subtypes.
 * <p>
 * Register handlers for item kinds (expression-based, {@code *}, or qualified {@code t.*}),
 * then resolve with a terminal method from {@link Match}.
 *
 * @param <R> the result type produced by the match
 */
public interface SelectItemMatch<R> extends Match<SelectItem, R> {

    /**
     * Creates a new matcher for the given {@link SelectItem}.
     *
     * @param i   the select item to match on
     * @param <R> the result type
     * @return a new {@code SelectItemMatch} for {@code i}
     */
    static <R> SelectItemMatch<R> match(SelectItem i) {
        return new SelectItemMatchImpl<>(i);
    }

    /**
     * Registers a handler for an expression-based select item.
     *
     * @param f handler for expression-based select items
     * @return {@code this} for fluent chaining
     */
    SelectItemMatch<R> expr(Function<ExprSelectItem, R> f);

    /**
     * Registers a handler for a {@code *} (star) select item.
     *
     * @param f handler for star select items
     * @return {@code this} for fluent chaining
     */
    SelectItemMatch<R> star(Function<StarSelectItem, R> f);

    /**
     * Registers a handler for a qualified {@code t.*} select item.
     *
     * @param f handler for qualified star select items
     * @return {@code this} for fluent chaining
     */
    SelectItemMatch<R> qualifiedStar(Function<QualifiedStarSelectItem, R> f);
}


