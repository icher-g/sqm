package io.sqm.catalog.model;

import java.util.*;

/**
 * Catalog table metadata.
 */
public final class CatalogTable {
    private final String schema;
    private final String name;
    private final Map<String, CatalogColumn> columnsByName;
    private final List<String> primaryKeyColumns;
    private final List<CatalogForeignKey> foreignKeys;
    private final boolean strictColumns;

    private CatalogTable(
        String schema,
        String name,
        List<CatalogColumn> columns,
        List<String> primaryKeyColumns,
        List<CatalogForeignKey> foreignKeys,
        boolean strictColumns
    ) {
        this.schema = schema;
        this.name = Objects.requireNonNull(name, "name");
        Objects.requireNonNull(columns, "columns");
        Objects.requireNonNull(primaryKeyColumns, "primaryKeyColumns");
        Objects.requireNonNull(foreignKeys, "foreignKeys");
        var map = new LinkedHashMap<String, CatalogColumn>(columns.size());
        for (var column : columns) {
            var normalized = normalize(column.name());
            if (map.putIfAbsent(normalized, column) != null) {
                throw new IllegalArgumentException("Duplicate column '" + column.name() + "' in table " + name);
            }
        }
        this.columnsByName = Collections.unmodifiableMap(map);
        this.primaryKeyColumns = List.copyOf(primaryKeyColumns);
        this.foreignKeys = List.copyOf(foreignKeys);
        this.strictColumns = strictColumns;
    }

    /**
     * Creates catalog table metadata.
     *
     * @param schema schema name, may be null.
     * @param name table name.
     * @param columns table columns.
     * @param primaryKeyColumns primary key column names.
     * @param foreignKeys foreign key metadata.
     * @return catalog table.
     */
    public static CatalogTable of(
        String schema,
        String name,
        List<CatalogColumn> columns,
        List<String> primaryKeyColumns,
        List<CatalogForeignKey> foreignKeys
    ) {
        return new CatalogTable(schema, name, columns, primaryKeyColumns, foreignKeys, true);
    }

    /**
     * Creates table metadata without key information.
     *
     * @param schema schema name.
     * @param name table name.
     * @param columns table columns.
     * @return catalog table.
     */
    public static CatalogTable of(String schema, String name, List<CatalogColumn> columns) {
        return new CatalogTable(schema, name, columns, List.of(), List.of(), true);
    }

    /**
     * Creates table metadata without key information.
     *
     * @param schema schema name.
     * @param name table name.
     * @param columns table columns.
     * @return catalog table.
     */
    public static CatalogTable of(String schema, String name, CatalogColumn... columns) {
        return of(schema, name, List.of(columns));
    }

    /**
     * Creates table metadata that accepts any column reference without explicit declarations.
     *
     * @param schema schema name, may be null.
     * @param name table name.
     * @return permissive catalog table.
     */
    public static CatalogTable allowingAnyColumns(String schema, String name) {
        return new CatalogTable(schema, name, List.of(), List.of(), List.of(), false);
    }

    /**
     * Returns schema name.
     *
     * @return schema name, may be null.
     */
    public String schema() {
        return schema;
    }

    /**
     * Returns table name.
     *
     * @return table name.
     */
    public String name() {
        return name;
    }

    /**
     * Returns table columns.
     *
     * @return immutable columns list.
     */
    public List<CatalogColumn> columns() {
        return List.copyOf(columnsByName.values());
    }

    /**
     * Finds a column by name.
     *
     * @param columnName column name.
     * @return optional column.
     */
    public Optional<CatalogColumn> column(String columnName) {
        return Optional.ofNullable(columnsByName.get(normalize(columnName)));
    }

    /**
     * Returns primary key columns in key order.
     *
     * @return immutable primary key columns list.
     */
    public List<String> primaryKeyColumns() {
        return primaryKeyColumns;
    }

    /**
     * Returns foreign key metadata.
     *
     * @return immutable foreign keys list.
     */
    public List<CatalogForeignKey> foreignKeys() {
        return foreignKeys;
    }

    /**
     * Returns whether column references should be validated strictly against declared metadata.
     *
     * @return true when the table exposes a closed column set.
     */
    public boolean strictColumns() {
        return strictColumns;
    }

    static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
