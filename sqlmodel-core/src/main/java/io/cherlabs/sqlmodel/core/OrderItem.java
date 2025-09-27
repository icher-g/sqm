package io.cherlabs.sqlmodel.core;

public record OrderItem(Column column, Direction direction, Nulls nulls, String collate) implements Entity {

    public static OrderItem of(Column column) {
        return new OrderItem(column, null, null, null);
    }

    public OrderItem asc() {
        return new OrderItem(column, Direction.ASC, nulls, collate);
    }

    public OrderItem desc() {
        return new OrderItem(column, Direction.DESC, nulls, collate);
    }

    public OrderItem nulls(Nulls nulls) {
        return new OrderItem(column, Direction.DESC, nulls, collate);
    }

    public OrderItem collate(String collate) {
        return new OrderItem(column, Direction.DESC, nulls, collate);
    }
}
