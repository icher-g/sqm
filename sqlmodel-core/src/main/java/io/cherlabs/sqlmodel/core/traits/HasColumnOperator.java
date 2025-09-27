package io.cherlabs.sqlmodel.core.traits;

import io.cherlabs.sqlmodel.core.ColumnFilter;

public interface HasColumnOperator {
    ColumnFilter.Operator operator();
}
