package io.sqm.validate.schema.model;

import java.util.*;

/**
 * Represents a database schema catalog used for query validation.
 */
public final class DbSchema {
    private final Map<TableKey, DbTable> tablesByKey;
    private final Map<String, List<DbTable>> tablesByName;

    private DbSchema(List<DbTable> tables) {
        Objects.requireNonNull(tables, "tables");
        var byKey = new LinkedHashMap<TableKey, DbTable>(tables.size());
        var byName = new LinkedHashMap<String, List<DbTable>>();
        for (var table : tables) {
            var key = new TableKey(normalize(table.schema()), normalize(table.name()));
            if (byKey.putIfAbsent(key, table) != null) {
                var printableSchema = table.schema() == null ? "<default>" : table.schema();
                throw new IllegalArgumentException("Duplicate table '" + printableSchema + "." + table.name() + "'");
            }
            byName.computeIfAbsent(normalize(table.name()), unused -> new ArrayList<>()).add(table);
        }
        this.tablesByKey = Collections.unmodifiableMap(byKey);
        var immutableByName = new LinkedHashMap<String, List<DbTable>>(byName.size());
        for (var entry : byName.entrySet()) {
            immutableByName.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        this.tablesByName = Collections.unmodifiableMap(immutableByName);
    }

    /**
     * Creates a schema model from tables.
     *
     * @param tables schema tables.
     * @return schema model.
     */
    public static DbSchema of(DbTable... tables) {
        return of(List.of(tables));
    }

    /**
     * Creates a schema model from tables.
     *
     * @param tables schema tables.
     * @return schema model.
     */
    public static DbSchema of(List<DbTable> tables) {
        return new DbSchema(tables);
    }

    /**
     * Returns all schema tables in declaration order.
     *
     * @return immutable list of tables.
     */
    public List<DbTable> tables() {
        return List.copyOf(tablesByKey.values());
    }

    /**
     * Resolves a table reference by schema/name.
     *
     * @param schema schema name, may be null.
     * @param name table name.
     * @return table lookup result.
     */
    public TableLookupResult resolve(String schema, String name) {
        var normalizedName = normalize(name);
        if (schema != null) {
            var table = tablesByKey.get(new TableKey(normalize(schema), normalizedName));
            return table == null ? TableLookupResult.notFound(schema, name) : TableLookupResult.found(table);
        }
        var matches = tablesByName.getOrDefault(normalizedName, List.of());
        if (matches.isEmpty()) {
            return TableLookupResult.notFound(null, name);
        }
        if (matches.size() > 1) {
            return TableLookupResult.ambiguous(name, matches);
        }
        return TableLookupResult.found(matches.getFirst());
    }

    private static String normalize(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    private record TableKey(String schema, String name) {
    }

    /**
     * Represents table lookup result.
     */
    public sealed interface TableLookupResult permits TableLookupResult.Found, TableLookupResult.NotFound, TableLookupResult.Ambiguous {
        /**
         * Creates successful lookup result.
         *
         * @param table resolved table.
         * @return found result.
         */
        static TableLookupResult found(DbTable table) {
            return new Found(table);
        }

        /**
         * Creates failed lookup result for missing table.
         *
         * @param schema schema name.
         * @param name table name.
         * @return not found result.
         */
        static TableLookupResult notFound(String schema, String name) {
            return new NotFound(schema, name);
        }

        /**
         * Creates failed lookup result for ambiguous table.
         *
         * @param name table name.
         * @param matches matching tables.
         * @return ambiguous result.
         */
        static TableLookupResult ambiguous(String name, List<DbTable> matches) {
            return new Ambiguous(name, List.copyOf(matches));
        }

        /**
         * Indicates whether lookup succeeded.
         *
         * @return {@code true} for successful resolution.
         */
        default boolean ok() {
            return this instanceof Found;
        }

        /**
         * Successful lookup result.
         *
         * @param table resolved table.
         */
        record Found(DbTable table) implements TableLookupResult {
        }

        /**
         * Missing table lookup result.
         *
         * @param schema schema name.
         * @param name table name.
         */
        record NotFound(String schema, String name) implements TableLookupResult {
        }

        /**
         * Ambiguous table lookup result.
         *
         * @param name requested table name.
         * @param matches matched tables.
         */
        record Ambiguous(String name, List<DbTable> matches) implements TableLookupResult {
        }
    }
}
