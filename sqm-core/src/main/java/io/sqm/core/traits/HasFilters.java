package io.sqm.core.traits;

import io.sqm.core.CompositeFilter;
import io.sqm.core.Filter;
import io.sqm.core.views.Filters;

import java.util.List;

/**
 * An interface to access {@link CompositeFilter#filters()} on top of {@link Filter} interface.
 * Use {@link Filters#filters(Filter)} to get them.
 */
public interface HasFilters {
    List<Filter> filters();
}
