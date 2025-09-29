package io.cherlabs.sqlmodel.core.traits;

import io.cherlabs.sqlmodel.core.Filter;
import io.cherlabs.sqlmodel.core.TupleFilter;

/**
 * An interface to get access to {@link TupleFilter#operator()} on top of {@link io.cherlabs.sqlmodel.core.Filter} interface.
 * Use {@link io.cherlabs.sqlmodel.core.views.Filters#tupleOperator(Filter)} to get it.
 */
public interface HasTupleOperator {
    TupleFilter.Operator operator();
}
