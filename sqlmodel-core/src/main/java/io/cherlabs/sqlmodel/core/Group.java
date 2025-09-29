package io.cherlabs.sqlmodel.core;

import java.util.Objects;

/**
 * Represents an item in a GroupBy statement.
 * <p><b>Note:</b> Only one of the fields can be set at a time.</p>
 *
 * @param column  a column used in a GroupBy statement.
 * @param ordinal an ordinal used in a GroupBy statement.
 */
public record Group(Column column, Integer ordinal) implements Entity {
    /**
     * Creates a group by item from column.
     *
     * @param expr a colum to group by
     * @return A newly created instance of a group item.
     */
    public static Group by(Column expr) {
        return new Group(Objects.requireNonNull(expr), null);
    }

    /**
     * Creates a group by item from ordinal.
     *
     * @param ordinal an ordinal to group by.
     * @return A newly created instance of a group item.
     */
    public static Group ofOrdinal(int ordinal) {
        return new Group(null, ordinal);
    }

    /**
     * Indicates if the {@link Group} is represented by ordinal.
     *
     * @return True if the group is represented by ordinal or False otherwise.
     */
    public boolean isOrdinal() {
        return ordinal != null;
    }
}
