package io.sqm.core.traits;

import io.sqm.core.Column;
import io.sqm.core.FunctionColumn;
import io.sqm.core.views.Columns;

import java.util.List;

/**
 * An interface to access function arguments on a {@link Column}.
 * Use {@link Columns#funcArgs(Column)} to get arguments.
 */
public interface HasArgs {
    List<FunctionColumn.Arg> args();
}
