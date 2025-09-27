package io.cherlabs.sqlmodel.core;

public interface Table extends Entity {
    static NamedTable of(String name) {
        return new NamedTable(name, null, null);
    }
}
