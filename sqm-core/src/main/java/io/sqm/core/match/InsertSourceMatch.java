package io.sqm.core.match;

import io.sqm.core.InsertSource;
import io.sqm.core.Query;
import io.sqm.core.RowExpr;
import io.sqm.core.RowListExpr;
import io.sqm.core.RowValues;

import java.util.function.Function;

/**
 * Pattern-style matcher for {@link InsertSource} subtypes.
 *
 * @param <R> result type
 */
public interface InsertSourceMatch<R> extends Match<InsertSource, R> {

    /**
     * Creates a matcher for the given insert source.
     *
     * @param source insert source
     * @param <R> result type
     * @return matcher instance
     */
    static <R> InsertSourceMatch<R> match(InsertSource source) {
        return new InsertSourceMatchImpl<>(source);
    }

    /**
     * Registers a handler for query sources.
     *
     * @param function query handler
     * @return this matcher
     */
    InsertSourceMatch<R> query(Function<Query, R> function);

    /**
     * Registers a handler for generic row-values sources.
     *
     * @param function row-values handler
     * @return this matcher
     */
    InsertSourceMatch<R> rowValues(Function<RowValues, R> function);

    /**
     * Registers a handler for single-row sources.
     *
     * @param function row handler
     * @return this matcher
     */
    InsertSourceMatch<R> row(Function<RowExpr, R> function);

    /**
     * Registers a handler for multi-row sources.
     *
     * @param function row-list handler
     * @return this matcher
     */
    InsertSourceMatch<R> rows(Function<RowListExpr, R> function);
}