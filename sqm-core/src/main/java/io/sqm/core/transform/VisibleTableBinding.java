package io.sqm.core.transform;

import java.util.Objects;

/**
 * A visible table source binding in a query scope used for column qualification resolution.
 *
 * @param schema table schema, may be {@code null}
 * @param tableName base table name
 * @param qualifier table alias if present, otherwise table name
 */
public record VisibleTableBinding(String schema, String tableName, String qualifier) {
    /**
     * Validates constructor arguments.
     *
     * @param schema table schema, may be {@code null}
     * @param tableName base table name
     * @param qualifier table alias if present, otherwise table name
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
     * @param qualifier table alias if present, otherwise table name
     * @return visible table binding
     */
    public static VisibleTableBinding of(String schema, String tableName, String qualifier) {
        return new VisibleTableBinding(schema, tableName, qualifier);
    }
}
