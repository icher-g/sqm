package io.sqm.core.traits;

import io.sqm.core.Column;
import io.sqm.core.FunctionColumn;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.core.views.Columns;
import io.sqm.core.views.Queries;

/**
 * An interface to get the {@link FunctionColumn#distinct()} boolean from the {@link Column} interface or
 * to get access to {@link SelectQuery#distinct()} from the {@link Query} interface.
 * Use {@link Columns#distinct(Column)} to get it.
 * Use {@link Queries#distinct(Query)} to get it.
 */
public interface HasDistinct {
    Boolean distinct();
}
