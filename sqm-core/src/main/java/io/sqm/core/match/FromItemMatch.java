package io.sqm.core.match;

import io.sqm.core.FromItem;
import io.sqm.core.Join;
import io.sqm.core.TableRef;

import java.util.function.Function;

/**
 * Type-safe pattern matching API for {@link FromItem} nodes.
 * <p>
 * This interface allows callers to declaratively match concrete {@link FromItem}
 * subtypes and associate them with handler functions, producing a result of type {@code R}.
 * Exactly one matching branch is expected to be selected.
 *
 * @param <R> the result type produced by the match
 */
public interface FromItemMatch<R> extends Match<FromItem, R> {

    /**
     * Creates a matcher for the given {@link FromItem}.
     *
     * @param i   the FROM item to match
     * @param <R> the result type
     * @return a new {@link FromItemMatch} instance
     */
    static <R> FromItemMatch<R> match(FromItem i) {
        return new FromItemMatchImpl<>(i);
    }

    /**
     * Matches a {@link Join}.
     *
     * @param f the function to apply when the item is a {@link Join}
     * @return this matcher for fluent chaining
     */
    FromItemMatch<R> join(Function<Join, R> f);

    /**
     * Matches a {@link TableRef}.
     *
     * @param f the function to apply when the item is a {@link TableRef}
     * @return this matcher for fluent chaining
     */
    FromItemMatch<R> tableRef(Function<TableRef, R> f);
}

