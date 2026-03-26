package io.sqm.core.match;

import io.sqm.core.Hint;
import io.sqm.core.StatementHint;
import io.sqm.core.TableHint;

import java.util.function.Function;

/**
 * Default matcher implementation for {@link Hint}.
 *
 * @param <R> result type
 */
public final class HintMatchImpl<R> implements HintMatch<R> {
    private final Hint hint;
    private boolean matched;
    private R result;

    /**
     * Creates a matcher.
     *
     * @param hint hint to match
     */
    public HintMatchImpl(Hint hint) {
        this.hint = hint;
    }

    @Override
    public HintMatch<R> statement(Function<StatementHint, R> function) {
        if (!matched && hint instanceof StatementHint statementHint) {
            result = function.apply(statementHint);
            matched = true;
        }
        return this;
    }

    @Override
    public HintMatch<R> table(Function<TableHint, R> function) {
        if (!matched && hint instanceof TableHint tableHint) {
            result = function.apply(tableHint);
            matched = true;
        }
        return this;
    }

    @Override
    public R otherwise(Function<Hint, R> function) {
        return matched ? result : function.apply(hint);
    }
}