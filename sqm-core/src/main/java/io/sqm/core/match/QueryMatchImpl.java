package io.sqm.core.match;

import io.sqm.core.CompositeQuery;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.core.WithQuery;

import java.util.function.Function;

/**
 * Default matcher implementation for {@link Query}.
 *
 * @param <R> result type
 */
public class QueryMatchImpl<R> implements QueryMatch<R> {

    private final Query query;
    private boolean matched = false;
    private R result;

    /**
     * Initializes a match builder for {@link Query}.
     *
     * @param query query to match
     */
    public QueryMatchImpl(Query query) {
        this.query = query;
    }

    @Override
    public QueryMatch<R> select(Function<SelectQuery, R> f) {
        if (!matched && query instanceof SelectQuery selectQuery) {
            result = f.apply(selectQuery);
            matched = true;
        }
        return this;
    }

    @Override
    public QueryMatch<R> with(Function<WithQuery, R> f) {
        if (!matched && query instanceof WithQuery withQuery) {
            result = f.apply(withQuery);
            matched = true;
        }
        return this;
    }

    @Override
    public QueryMatch<R> composite(Function<CompositeQuery, R> f) {
        if (!matched && query instanceof CompositeQuery compositeQuery) {
            result = f.apply(compositeQuery);
            matched = true;
        }
        return this;
    }

    @Override
    public R otherwise(Function<Query, R> f) {
        return matched ? result : f.apply(query);
    }
}
