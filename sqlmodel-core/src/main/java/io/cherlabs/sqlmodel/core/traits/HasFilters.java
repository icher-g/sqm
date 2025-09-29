package io.cherlabs.sqlmodel.core.traits;

import io.cherlabs.sqlmodel.core.CompositeFilter;
import io.cherlabs.sqlmodel.core.Filter;

import java.util.List;

/**
 * An interface to access {@link CompositeFilter#filters()} on top of {@link Filter} interface.
 * Use {@link io.cherlabs.sqlmodel.core.views.Filters#filters(Filter)} to get them.
 */
public interface HasFilters {
    List<Filter> filters();
}
