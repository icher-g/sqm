package io.cherlabs.sqlmodel.core;

import java.util.Objects;

public record GroupItem(Column column, Integer ordinal) implements Entity {
    public static GroupItem of(Column expr) {
        return new GroupItem(Objects.requireNonNull(expr), null);
    }

    public static GroupItem ofOrdinal(int ordinal) {
        return new GroupItem(null, ordinal);
    }

    public boolean isOrdinal() {
        return ordinal != null;
    }

    public boolean isExpression() {
        return column != null;
    }
}
