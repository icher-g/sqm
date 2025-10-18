package io.sqm.core.traits;

import io.sqm.core.CteQuery;
import io.sqm.core.Query;
import io.sqm.core.views.Queries;

import java.util.List;

/**
 * An interface to get access to {@link CteQuery#columnAliases()}.
 * Use {@link Queries#columnAliases(Query)} to get them.
 */
public interface HasColumnAliases {
    List<String> columnAliases();
}
