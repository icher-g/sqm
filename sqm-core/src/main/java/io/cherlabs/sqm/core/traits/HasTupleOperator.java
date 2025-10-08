package io.cherlabs.sqm.core.traits;

import io.cherlabs.sqm.core.Filter;
import io.cherlabs.sqm.core.TupleFilter;

/**
 * An interface to get access to {@link TupleFilter#operator()} on top of {@link io.cherlabs.sqm.core.Filter} interface.
 * Use {@link io.cherlabs.sqm.core.views.Filters#tupleOperator(Filter)} to get it.
 */
public interface HasTupleOperator {
    TupleFilter.Operator operator();
}
