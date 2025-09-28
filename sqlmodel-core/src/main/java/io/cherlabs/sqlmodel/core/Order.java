package io.cherlabs.sqlmodel.core;

public record Order(Column column, Direction direction, Nulls nulls, String collate) implements Entity {

    public static Order of(Column column) {
        return new Order(column, null, null, null);
    }

    public Order asc() {
        return new Order(column, Direction.ASC, nulls, collate);
    }

    public Order desc() {
        return new Order(column, Direction.DESC, nulls, collate);
    }

    public Order nulls(Nulls nulls) {
        return new Order(column, direction, nulls, collate);
    }

    public Order collate(String collate) {
        return new Order(column, direction, nulls, collate);
    }
}
