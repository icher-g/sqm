package io.cherlabs.sqlmodel.core.traits;

import io.cherlabs.sqlmodel.core.Column;
import io.cherlabs.sqlmodel.core.FunctionColumn;

import java.util.List;

/**
 * An interface to access function arguments on a {@link io.cherlabs.sqlmodel.core.Column}.
 * Use {@link io.cherlabs.sqlmodel.core.views.Columns#funcArgs(Column)} to get arguments.
 */
public interface HasArgs {
    List<FunctionColumn.Arg> args();
}
