package io.sqm.core.traits;

import io.sqm.core.CompositeFilter;
import io.sqm.core.Filter;
import io.sqm.core.views.Filters;

/**
 * An interface to access composite filter op on {@link Filter}.
 * Use {@link Filters#compositeOperator(Filter)} to get it.
 */
public interface HasCompositeOperator {
    CompositeFilter.Operator op();
}
