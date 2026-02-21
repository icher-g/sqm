package io.sqm.catalog.model;

import java.util.*;

/**
 * Catalog schema metadata.
 */
public final class CatalogSchema {
    private final Map<TableKey, CatalogTable> tablesByKey;
    private final Map<String, List<CatalogTable>> tablesByName;

    private CatalogSchema(List<CatalogTable> tables) {
        Objects.requireNonNull(tables, "tables");
        var byKey = new LinkedHashMap<TableKey, CatalogTable>(tables.size());
        var byName = new LinkedHashMap<String, List<CatalogTable>>();
        for (var table : tables) {
            var key = new TableKey(normalize(table.schema()), normalize(table.name()));
            if (byKey.putIfAbsent(key, table) != null) {
                var printableSchema = table.schema() == null ? "<default>" : table.schema();
                throw new IllegalArgumentException("Duplicate table '" + printableSchema + "." + table.name() + "'");
            }
            byName.computeIfAbsent(normalize(table.name()), unused -> new ArrayList<>()).add(table);
        }
        this.tablesByKey = Collections.unmodifiableMap(byKey);
        var immutableByName = new LinkedHashMap<String, List<CatalogTable>>(byName.size());
        for (var entry : byName.entrySet()) {
            immutableByName.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        this.tablesByName = Collections.unmodifiableMap(immutableByName);
    }

    /**
     * Creates catalog schema from table list.
     *
     * @param tables catalog tables.
     * @return catalog schema.
     */
    public static CatalogSchema of(List<CatalogTable> tables) {
        return new CatalogSchema(tables);
    }

    /**
     * Creates catalog schema from table list.
     *
     * @param tables catalog tables.
     * @return catalog schema.
     */
    public static CatalogSchema of(CatalogTable... tables) {
        return of(List.of(tables));
    }

    /**
     * Returns tables in declaration order.
     *
     * @return immutable table list.
     */
    public List<CatalogTable> tables() {
        return List.copyOf(tablesByKey.values());
    }

    /**
     * Resolves table by schema and name.
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
     * Table lookup result.
     */
    public sealed interface TableLookupResult permits TableLookupResult.Found, TableLookupResult.NotFound, TableLookupResult.Ambiguous {
        /**
         * Creates successful lookup result.
         *
         * @param table resolved table.
         * @return found result.
         */
        static TableLookupResult found(CatalogTable table) {
            return new Found(table);
        }

        /**
         * Creates not-found lookup result.
         *
         * @param schema schema name.
         * @param name table name.
         * @return not-found result.
         */
        static TableLookupResult notFound(String schema, String name) {
            return new NotFound(schema, name);
        }

        /**
         * Creates ambiguous lookup result.
         *
         * @param name table name.
         * @param matches matching tables.
         * @return ambiguous result.
         */
        static TableLookupResult ambiguous(String name, List<CatalogTable> matches) {
            return new Ambiguous(name, List.copyOf(matches));
        }

        /**
         * Returns whether lookup succeeded.
         *
         * @return true when found.
         */
        default boolean ok() {
            return this instanceof Found;
        }

        /**
         * Found table result.
         *
         * @param table resolved table.
         */
        record Found(CatalogTable table) implements TableLookupResult {
        }

        /**
         * Missing table result.
         *
         * @param schema schema name.
         * @param name table name.
         */
        record NotFound(String schema, String name) implements TableLookupResult {
        }

        /**
         * Ambiguous table result.
         *
         * @param name table name.
         * @param matches matching tables.
         */
        record Ambiguous(String name, List<CatalogTable> matches) implements TableLookupResult {
        }
    }
}
