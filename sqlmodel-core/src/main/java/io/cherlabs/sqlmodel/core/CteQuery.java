package io.cherlabs.sqlmodel.core;

import java.util.ArrayList;
import java.util.List;

public class CteQuery extends Query<CteQuery> {
    private final List<String> columnAliases;

    public CteQuery() {
        this.columnAliases = new ArrayList<>();
    }

    public List<String> columnAliases() {
        return columnAliases;
    }

    public CteQuery columnAliases(String... columnAliases) {
        this.columnAliases.addAll(List.of(columnAliases));
        return this;
    }
}
