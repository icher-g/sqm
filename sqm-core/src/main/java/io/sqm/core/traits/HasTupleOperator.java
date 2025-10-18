package io.sqm.core.traits;

import io.sqm.core.Filter;
import io.sqm.core.TupleFilter;
import io.sqm.core.views.Filters;

/**
 * An interface to get access to {@link TupleFilter#operator()} on top of {@link Filter} interface.
 * Use {@link Filters#tupleOperator(Filter)} to get it.
 */
public interface HasTupleOperator {
    TupleFilter.Operator operator();
}
