package io.sqm.core.match;

import io.sqm.core.InsertSource;
import io.sqm.core.Query;
import io.sqm.core.RowExpr;
import io.sqm.core.RowListExpr;
import io.sqm.core.RowValues;

import java.util.function.Function;

/**
 * Default matcher implementation for {@link InsertSource}.
 *
 * @param <R> result type
 */
public final class InsertSourceMatchImpl<R> implements InsertSourceMatch<R> {

    private final InsertSource source;
    private boolean matched;
    private R result;

    /**
     * Creates an insert-source matcher.
     *
     * @param source insert source
     */
    public InsertSourceMatchImpl(InsertSource source) {
        this.source = source;
    }

    @Override
    public InsertSourceMatch<R> query(Function<Query, R> function) {
        if (!matched && source instanceof Query query) {
            result = function.apply(query);
            matched = true;
        }
        return this;
    }

    @Override
    public InsertSourceMatch<R> rowValues(Function<RowValues, R> function) {
        if (!matched && source instanceof RowValues rowValues) {
            result = function.apply(rowValues);
            matched = true;
        }
        return this;
    }

    @Override
    public InsertSourceMatch<R> row(Function<RowExpr, R> function) {
        if (!matched && source instanceof RowExpr rowExpr) {
            result = function.apply(rowExpr);
            matched = true;
        }
        return this;
    }

    @Override
    public InsertSourceMatch<R> rows(Function<RowListExpr, R> function) {
        if (!matched && source instanceof RowListExpr rowListExpr) {
            result = function.apply(rowListExpr);
            matched = true;
        }
        return this;
    }

    @Override
    public R otherwise(Function<InsertSource, R> function) {
        return matched ? result : function.apply(source);
    }
}