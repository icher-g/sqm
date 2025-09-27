package io.cherlabs.sqlmodel.core;

import io.cherlabs.sqlmodel.core.traits.HasAlias;
import io.cherlabs.sqlmodel.core.traits.HasName;
import io.cherlabs.sqlmodel.core.traits.HasTableName;

public record NamedColumn(String name, String alias, String table) implements Column, HasName, HasAlias, HasTableName {
    public NamedColumn as(String alias) {
        return new NamedColumn(name, alias, table);
    }

    public NamedColumn from(String table) {
        return new NamedColumn(name, alias, table);
    }
}
