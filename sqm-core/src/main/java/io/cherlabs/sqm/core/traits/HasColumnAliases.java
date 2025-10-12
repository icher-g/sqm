package io.cherlabs.sqm.core.traits;

import io.cherlabs.sqm.core.CteQuery;
import io.cherlabs.sqm.core.Query;

import java.util.List;

/**
 * An interface to get access to {@link CteQuery#columnAliases()}.
 * Use {@link io.cherlabs.sqm.core.views.Queries#columnAliases(Query)} to get them.
 */
public interface HasColumnAliases {
    List<String> columnAliases();
}
