package io.cherlabs.sqlmodel.core.traits;

import io.cherlabs.sqlmodel.core.CompositeFilter;

public interface HasCompositeOperator {
    CompositeFilter.Operator operator();
}
