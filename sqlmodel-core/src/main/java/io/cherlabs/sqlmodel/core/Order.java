package io.cherlabs.sqlmodel.core;

/**
 * Represents an OrderBy item.
 *
 * @param column    a column to be ordered by.
 * @param direction an order by direction per column.
 * @param nulls     nulls to be used in OrderBy clause.
 * @param collate   collate to be used in OrderBy clause.
 */
public record Order(Column column, Direction direction, Nulls nulls, String collate) implements Entity {

    /**
     * Creates an order by item for a provided column.
     *
     * @param column a column to be used in an OrderBy clause.
     * @return A newly created instance of an order by item.
     */
    public static Order by(Column column) {
        return new Order(column, null, null, null);
    }

    /**
     * Adds a {@link Direction#Asc} direction to an order by item.
     *
     * @return A new instance of the order item with the provided direction. All other fields are preserved.
     */
    public Order asc() {
        return new Order(column, Direction.Asc, nulls, collate);
    }

    /**
     * Adds a {@link Direction#Desc} direction to an order by item.
     *
     * @return A new instance of the order item with the provided direction. All other fields are preserved.
     */
    public Order desc() {
        return new Order(column, Direction.Desc, nulls, collate);
    }

    /**
     * Adds {@link Nulls} to an order by item.
     *
     * @param nulls nulls to be used in OrderBy clause.
     * @return A new instance of the order item with the provided nulls. All other fields are preserved.
     */
    public Order nulls(Nulls nulls) {
        return new Order(column, direction, nulls, collate);
    }

    /**
     * Adds collate to an order by item.
     *
     * @param collate collate to be used in OrderBy clause.
     * @return A new instance of the order item with the provided collate. All other fields are preserved.
     */
    public Order collate(String collate) {
        return new Order(column, direction, nulls, collate);
    }
}
