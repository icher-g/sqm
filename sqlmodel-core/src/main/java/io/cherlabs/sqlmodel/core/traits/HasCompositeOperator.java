package io.cherlabs.sqlmodel.core.traits;

import io.cherlabs.sqlmodel.core.CompositeFilter;
import io.cherlabs.sqlmodel.core.Filter;

/**
 * An interface to access composite filter op on {@link io.cherlabs.sqlmodel.core.Filter}.
 * Use {@link io.cherlabs.sqlmodel.core.views.Filters#compositeOperator(Filter)} to get it.
 */
public interface HasCompositeOperator {
    CompositeFilter.Operator op();
}
