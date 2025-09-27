package io.cherlabs.sqlmodel.core;

import io.cherlabs.sqlmodel.core.traits.HasAlias;
import io.cherlabs.sqlmodel.core.traits.HasQuery;

public record QueryColumn(Query query, String alias) implements Column, HasQuery, HasAlias {
    public QueryColumn as(String alias) {
        return new QueryColumn(query, alias);
    }
}
