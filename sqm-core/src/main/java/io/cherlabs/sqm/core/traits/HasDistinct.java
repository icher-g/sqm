package io.cherlabs.sqm.core.traits;

import io.cherlabs.sqm.core.Column;
import io.cherlabs.sqm.core.FunctionColumn;
import io.cherlabs.sqm.core.Query;
import io.cherlabs.sqm.core.SelectQuery;

/**
 * An interface to get the {@link FunctionColumn#distinct()} boolean from the {@link io.cherlabs.sqm.core.Column} interface or
 * to get access to {@link SelectQuery#distinct()} from the {@link io.cherlabs.sqm.core.Query} interface.
 * Use {@link io.cherlabs.sqm.core.views.Columns#distinct(Column)} to get it.
 * Use {@link io.cherlabs.sqm.core.views.Queries#distinct(Query)} to get it.
 */
public interface HasDistinct {
    Boolean distinct();
}
