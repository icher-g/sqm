package io.cherlabs.sqlmodel.core;

import io.cherlabs.sqlmodel.core.traits.HasAlias;
import io.cherlabs.sqlmodel.core.traits.HasQuery;

public record QueryTable(Query query, String alias) implements Table, HasQuery, HasAlias {
}
