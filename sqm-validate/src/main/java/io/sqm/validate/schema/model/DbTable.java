package io.sqm.validate.schema.model;

import java.util.*;

/**
 * Represents a database table in a validation schema.
 */
public final class DbTable {
    private final String schema;
    private final String name;
    private final Map<String, DbColumn> columnsByName;

    private DbTable(String schema, String name, List<DbColumn> columns) {
        this.schema = schema;
        this.name = Objects.requireNonNull(name, "name");
        Objects.requireNonNull(columns, "columns");
        var map = new LinkedHashMap<String, DbColumn>(columns.size());
        for (var column : columns) {
            var normalized = normalize(column.name());
            if (map.putIfAbsent(normalized, column) != null) {
                throw new IllegalArgumentException("Duplicate column '" + column.name() + "' in table " + name);
            }
        }
        this.columnsByName = Collections.unmodifiableMap(map);
    }

    /**
     * Creates a table model.
     *
     * @param name table name.
     * @param columns table columns.
     * @return table model.
     */
    public static DbTable of(String name, DbColumn... columns) {
        return of(null, name, List.of(columns));
    }

    /**
     * Creates a table model.
     *
     * @param schema schema name, may be null.
     * @param name table name.
     * @param columns table columns.
     * @return table model.
     */
    public static DbTable of(String schema, String name, DbColumn... columns) {
        return of(schema, name, List.of(columns));
    }

    /**
     * Creates a table model.
     *
     * @param schema schema name, may be null.
     * @param name table name.
     * @param columns table columns.
     * @return table model.
     */
    public static DbTable of(String schema, String name, List<DbColumn> columns) {
        return new DbTable(schema, name, columns);
    }

    /**
     * Returns schema name.
     *
     * @return schema name or null.
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
     * Returns all columns.
     *
     * @return immutable columns list.
     */
    public List<DbColumn> columns() {
        return List.copyOf(columnsByName.values());
    }

    /**
     * Finds a column by name.
     *
     * @param columnName column name.
     * @return optional column.
     */
    public Optional<DbColumn> column(String columnName) {
        return Optional.ofNullable(columnsByName.get(normalize(columnName)));
    }

    /**
     * Returns columns indexed by normalized name.
     *
     * @return immutable column map.
     */
    public Map<String, DbColumn> columnsByName() {
        return columnsByName;
    }

    static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
