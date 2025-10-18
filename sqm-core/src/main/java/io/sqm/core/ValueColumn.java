package io.sqm.core;

/**
 * Represents an value as a column.
 *
 * @param value a value.
 * @param alias an alias.
 */
public record ValueColumn(Object value, String alias) implements Column {
    /**
     * Adds an alias to the column.
     *
     * @param alias an alias.
     * @return A new instance of the column with the alias. All other fields are preserved.
     */
    public ValueColumn as(String alias) {
        return new ValueColumn(value, alias);
    }
}
