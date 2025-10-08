package io.cherlabs.sqm.core.traits;

import io.cherlabs.sqm.core.CompositeFilter;
import io.cherlabs.sqm.core.Filter;

/**
 * An interface to access composite filter op on {@link io.cherlabs.sqm.core.Filter}.
 * Use {@link io.cherlabs.sqm.core.views.Filters#compositeOperator(Filter)} to get it.
 */
public interface HasCompositeOperator {
    CompositeFilter.Operator op();
}
