package io.cherlabs.sqm.core.traits;

import io.cherlabs.sqm.core.CompositeFilter;
import io.cherlabs.sqm.core.Filter;

import java.util.List;

/**
 * An interface to access {@link CompositeFilter#filters()} on top of {@link Filter} interface.
 * Use {@link io.cherlabs.sqm.core.views.Filters#filters(Filter)} to get them.
 */
public interface HasFilters {
    List<Filter> filters();
}
