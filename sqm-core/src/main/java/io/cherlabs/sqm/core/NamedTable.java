package io.cherlabs.sqm.core;

import io.cherlabs.sqm.core.traits.HasAlias;
import io.cherlabs.sqm.core.traits.HasName;
import io.cherlabs.sqm.core.traits.HasSchema;

/**
 * Represents a table with schema, name and alias.
 * For example: {@code dbo.sales AS sls }
 *
 * @param name   a table name.
 * @param alias  a table alias.
 * @param schema a table schema.
 */
public record NamedTable(String name, String alias, String schema) implements Table, HasName, HasAlias, HasSchema {
    /**
     * Adds an alias to the table.
     *
     * @param alias an alias.
     * @return A new instance of the table with the provided alias. All other fields are preserved.
     */
    public NamedTable as(String alias) {
        return new NamedTable(name, alias, schema);
    }

    /**
     * Adds a schema to the table.
     *
     * @param schema a schema.
     * @return A new instance of the table with the provided schema. All other fields are preserved.
     */
    public NamedTable from(String schema) {
        return new NamedTable(name, alias, schema);
    }
}
