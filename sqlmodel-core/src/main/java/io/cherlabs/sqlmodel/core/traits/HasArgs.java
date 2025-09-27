package io.cherlabs.sqlmodel.core.traits;

import io.cherlabs.sqlmodel.core.FunctionColumn;

import java.util.List;

public interface HasArgs {
    List<FunctionColumn.Arg> args();
}
