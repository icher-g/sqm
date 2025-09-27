package io.cherlabs.sqlmodel.core.traits;

import io.cherlabs.sqlmodel.core.Filter;

import java.util.List;

public interface HasFilters {
    List<Filter> filters();
}
