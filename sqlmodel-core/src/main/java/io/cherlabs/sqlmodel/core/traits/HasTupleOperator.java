package io.cherlabs.sqlmodel.core.traits;

import io.cherlabs.sqlmodel.core.TupleFilter;

public interface HasTupleOperator {
    TupleFilter.Operator operator();
}
