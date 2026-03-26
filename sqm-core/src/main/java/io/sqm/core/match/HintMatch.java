package io.sqm.core.match;

import io.sqm.core.Hint;
import io.sqm.core.StatementHint;
import io.sqm.core.TableHint;

import java.util.function.Function;

/**
 * Pattern-style matcher for {@link Hint} variants.
 *
 * @param <R> result type
 */
public interface HintMatch<R> extends Match<Hint, R> {

    /**
     * Creates a matcher for the given hint.
     *
     * @param hint hint to match
     * @param <R> result type
     * @return matcher
     */
    static <R> HintMatch<R> match(Hint hint) {
        return new HintMatchImpl<>(hint);
    }

    /**
     * Registers a handler for generic statement hints.
     *
     * @param function handler
     * @return this matcher
     */
    HintMatch<R> statement(Function<StatementHint, R> function);

    /**
     * Registers a handler for generic table hints.
     *
     * @param function handler
     * @return this matcher
     */
    HintMatch<R> table(Function<TableHint, R> function);
}