package io.cherlabs.sqm.core;

/**
 * A base interface for all table implementations.
 */
public interface Table extends Entity {
    /**
     * Creates a table with the provided name.
     *
     * @param name a name of the table.
     * @return A new instance of the table.
     */
    static NamedTable of(String name) {
        return new NamedTable(name, null, null);
    }

    /**
     * Creates a table that is based on a sub query.
     *
     * @param query a sub query.
     * @return A new instance of the table.
     */
    static QueryTable of(Query query) {
        return new QueryTable(query, null);
    }
}
