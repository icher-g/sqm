package io.cherlabs.sqlmodel.core;

import java.util.Objects;

public record Group(Column column, Integer ordinal) implements Entity {
    public static Group of(Column expr) {
        return new Group(Objects.requireNonNull(expr), null);
    }

    public static Group ofOrdinal(int ordinal) {
        return new Group(null, ordinal);
    }

    public boolean isOrdinal() {
        return ordinal != null;
    }

    public boolean isExpression() {
        return column != null;
    }
}
