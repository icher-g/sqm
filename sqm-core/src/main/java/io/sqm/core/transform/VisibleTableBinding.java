package io.sqm.core.transform;

import io.sqm.core.Identifier;

import java.util.Objects;

/**
 * A visible table source binding in a query scope used for column qualification resolution.
 *
 * @param schema table schema, may be {@code null}
 * @param tableName base table name
 * @param qualifier table alias if present, otherwise table name identifier
 */
public record VisibleTableBinding(String schema, String tableName, Identifier qualifier) {
    /**
     * Validates constructor arguments.
     *
     * @param schema table schema, may be {@code null}
     * @param tableName base table name
     * @param qualifier table alias if present, otherwise table name identifier
     */
    public VisibleTableBinding {
        Objects.requireNonNull(tableName, "tableName");
        Objects.requireNonNull(qualifier, "qualifier");
    }

    /**
     * Creates a visible table binding.
     *
     * @param schema table schema, may be {@code null}
     * @param tableName base table name
     * @param qualifier table alias if present, otherwise table name identifier
     * @return visible table binding
     */
    public static VisibleTableBinding of(String schema, String tableName, Identifier qualifier) {
        return new VisibleTableBinding(schema, tableName, qualifier);
    }
}
