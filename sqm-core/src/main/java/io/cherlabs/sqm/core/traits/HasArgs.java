package io.cherlabs.sqm.core.traits;

import io.cherlabs.sqm.core.Column;
import io.cherlabs.sqm.core.FunctionColumn;

import java.util.List;

/**
 * An interface to access function arguments on a {@link io.cherlabs.sqm.core.Column}.
 * Use {@link io.cherlabs.sqm.core.views.Columns#funcArgs(Column)} to get arguments.
 */
public interface HasArgs {
    List<FunctionColumn.Arg> args();
}
