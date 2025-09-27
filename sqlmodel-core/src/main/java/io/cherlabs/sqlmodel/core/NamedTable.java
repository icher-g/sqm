package io.cherlabs.sqlmodel.core;

import io.cherlabs.sqlmodel.core.traits.HasAlias;
import io.cherlabs.sqlmodel.core.traits.HasName;
import io.cherlabs.sqlmodel.core.traits.HasSchema;

public record NamedTable(String name, String alias, String schema) implements Table, HasName, HasAlias, HasSchema {
    public NamedTable as(String alias) {
        return new NamedTable(name, alias, schema);
    }

    public NamedTable from(String schema) {
        return new NamedTable(name, alias, schema);
    }
}
