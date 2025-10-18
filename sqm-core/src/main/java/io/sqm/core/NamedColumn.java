package io.sqm.core;

import io.sqm.core.traits.HasAlias;
import io.sqm.core.traits.HasName;
import io.sqm.core.traits.HasTableName;

/**
 * Represents a column with name, table and alias.
 * For example: {@code Sales.ID AS SID }.
 *
 * @param name  the name of the column.
 * @param alias the alias for the column.
 * @param table the table the column belongs to.
 */
public record NamedColumn(String name, String alias, String table) implements Column, HasName, HasAlias, HasTableName {
    /**
     * Adds an alias to the column.
     *
     * @param alias an alias.
     * @return A new instance of the column with the alias. All other fields are preserved.
     */
    public NamedColumn as(String alias) {
        return new NamedColumn(name, alias, table);
    }

    /**
     * Adds a table to the column.
     *
     * @param table a table.
     * @return A new instance of the column with the table. All other fields are preserved.
     */
    public NamedColumn from(String table) {
        return new NamedColumn(name, alias, table);
    }
}
