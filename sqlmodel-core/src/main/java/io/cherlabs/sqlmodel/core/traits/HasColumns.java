package io.cherlabs.sqlmodel.core.traits;

import io.cherlabs.sqlmodel.core.Column;

import java.util.List;

public interface HasColumns {
    List<Column> columns();
}
