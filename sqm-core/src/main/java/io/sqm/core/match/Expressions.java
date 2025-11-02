package io.sqm.core.match;

import io.sqm.core.Expression;

public final class Expressions {
    private Expressions() {
    }

    /**
     * Creates a new matcher for the given {@link Expression}.
     *
     * @param e   the expression to match on (may be any concrete {@code Expression} subtype)
     * @param <R> the result type produced by the match
     * @return a new {@code ExpressionMatch} for {@code e}
     */
    static <R> ExpressionMatch<R> match(Expression e) {
        return ExpressionMatch.match(e);
    }
}
