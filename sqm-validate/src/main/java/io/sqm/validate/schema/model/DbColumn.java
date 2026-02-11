package io.sqm.validate.schema.model;

import java.util.Objects;

/**
 * Represents a database column in a validation schema.
 *
 * @param name column name.
 * @param type semantic column type.
 */
public record DbColumn(String name, DbType type) {
    /**
     * Creates a new database column model.
     *
     * @param name column name.
     * @param type semantic column type.
     */
    public DbColumn {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
    }

    /**
     * Creates a column with the provided name and type.
     *
     * @param name column name.
     * @param type semantic column type.
     * @return column model.
     */
    public static DbColumn of(String name, DbType type) {
        return new DbColumn(name, type);
    }
}
