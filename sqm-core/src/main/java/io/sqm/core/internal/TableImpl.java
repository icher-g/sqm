package io.sqm.core.internal;

import io.sqm.core.Table;

public record TableImpl(String schema, String name, String alias) implements Table {
}
