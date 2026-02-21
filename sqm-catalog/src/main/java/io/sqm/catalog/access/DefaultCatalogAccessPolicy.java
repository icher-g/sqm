package io.sqm.catalog.access;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Default catalog access policy with optional principal-specific overrides.
 *
 * <p>All keys are matched case-insensitively.</p>
 */
public final class DefaultCatalogAccessPolicy implements CatalogAccessPolicy {
    private static final DefaultCatalogAccessPolicy ALLOW_ALL = builder().build();

    private final Set<String> deniedTables;
    private final Set<String> deniedColumns;
    private final Set<String> allowedFunctions;
    private final Map<String, Set<String>> deniedTablesByPrincipal;
    private final Map<String, Set<String>> deniedColumnsByPrincipal;
    private final Map<String, Set<String>> allowedFunctionsByPrincipal;

    private DefaultCatalogAccessPolicy(
        Set<String> deniedTables,
        Set<String> deniedColumns,
        Set<String> allowedFunctions,
        Map<String, Set<String>> deniedTablesByPrincipal,
        Map<String, Set<String>> deniedColumnsByPrincipal,
        Map<String, Set<String>> allowedFunctionsByPrincipal
    ) {
        this.deniedTables = normalizeAll(deniedTables);
        this.deniedColumns = normalizeAll(deniedColumns);
        this.allowedFunctions = normalizeAll(allowedFunctions);
        this.deniedTablesByPrincipal = normalizePrincipalMap(deniedTablesByPrincipal);
        this.deniedColumnsByPrincipal = normalizePrincipalMap(deniedColumnsByPrincipal);
        this.allowedFunctionsByPrincipal = normalizePrincipalMap(allowedFunctionsByPrincipal);
    }

    /**
     * Returns allow-all policy.
     *
     * @return allow-all policy.
     */
    public static DefaultCatalogAccessPolicy allowAll() {
        return ALLOW_ALL;
    }

    /**
     * Creates a mutable builder.
     *
     * @return policy builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns true when a table is denied for the principal.
     *
     * @param principal principal identifier, may be {@code null}.
     * @param schemaName table schema, may be {@code null}.
     * @param tableName table name.
     * @return true when denied.
     */
    @Override
    public boolean isTableDenied(String principal, String schemaName, String tableName) {
        if (tableName == null) {
            return false;
        }
        var table = normalize(tableName);
        if (containsTable(deniedTables, schemaName, table)) {
            return true;
        }
        var principalRules = rulesForPrincipal(deniedTablesByPrincipal, principal);
        return containsTable(principalRules, schemaName, table);
    }

    /**
     * Returns true when a column is denied for the principal.
     *
     * @param principal principal identifier, may be {@code null}.
     * @param sourceName source alias or table name, may be {@code null}.
     * @param columnName column name.
     * @return true when denied.
     */
    @Override
    public boolean isColumnDenied(String principal, String sourceName, String columnName) {
        if (columnName == null) {
            return false;
        }
        var column = normalize(columnName);
        if (containsColumn(deniedColumns, sourceName, column)) {
            return true;
        }
        var principalRules = rulesForPrincipal(deniedColumnsByPrincipal, principal);
        return containsColumn(principalRules, sourceName, column);
    }

    /**
     * Returns true when a function is allowed for the principal.
     *
     * @param principal principal identifier, may be {@code null}.
     * @param functionName function name.
     * @return true when allowed.
     */
    @Override
    public boolean isFunctionAllowed(String principal, String functionName) {
        if (functionName == null) {
            return true;
        }
        var principalRules = rulesForPrincipal(allowedFunctionsByPrincipal, principal);
        var hasAllowlists = !allowedFunctions.isEmpty() || !allowedFunctionsByPrincipal.isEmpty();
        if (!hasAllowlists) {
            return true;
        }
        var normalized = normalize(functionName);
        return allowedFunctions.contains(normalized) || principalRules.contains(normalized);
    }

    /**
     * Returns denied table keys.
     *
     * @return immutable set.
     */
    public Set<String> deniedTables() {
        return deniedTables;
    }

    /**
     * Returns denied column keys.
     *
     * @return immutable set.
     */
    public Set<String> deniedColumns() {
        return deniedColumns;
    }

    /**
     * Returns allowed function names.
     *
     * @return immutable set.
     */
    public Set<String> allowedFunctions() {
        return allowedFunctions;
    }

    private static boolean containsTable(Set<String> rules, String schemaName, String table) {
        if (rules.contains(table)) {
            return true;
        }
        if (schemaName == null || schemaName.isBlank()) {
            return false;
        }
        return rules.contains(normalize(schemaName) + "." + table);
    }

    private static boolean containsColumn(Set<String> rules, String sourceName, String column) {
        if (rules.contains(column)) {
            return true;
        }
        if (sourceName == null || sourceName.isBlank()) {
            return false;
        }
        return rules.contains(normalize(sourceName) + "." + column);
    }

    private static Set<String> rulesForPrincipal(Map<String, Set<String>> rulesByPrincipal, String principal) {
        if (principal == null || principal.isBlank()) {
            return Set.of();
        }
        return rulesByPrincipal.getOrDefault(normalize(principal), Set.of());
    }

    private static Map<String, Set<String>> normalizePrincipalMap(Map<String, Set<String>> map) {
        Objects.requireNonNull(map, "map");
        var normalized = new LinkedHashMap<String, Set<String>>(map.size());
        for (var entry : map.entrySet()) {
            var principal = entry.getKey();
            if (principal == null || principal.isBlank()) {
                continue;
            }
            normalized.put(normalize(principal), normalizeAll(entry.getValue()));
        }
        return Map.copyOf(normalized);
    }

    private static Set<String> normalizeAll(Set<String> values) {
        Objects.requireNonNull(values, "values");
        var normalized = new LinkedHashSet<String>(values.size());
        for (var value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            normalized.add(normalize(value));
        }
        return Set.copyOf(normalized);
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    /**
     * Mutable builder for {@link DefaultCatalogAccessPolicy}.
     */
    public static final class Builder {
        private final Set<String> deniedTables = new LinkedHashSet<>();
        private final Set<String> deniedColumns = new LinkedHashSet<>();
        private final Set<String> allowedFunctions = new LinkedHashSet<>();
        private final Map<String, Set<String>> deniedTablesByPrincipal = new LinkedHashMap<>();
        private final Map<String, Set<String>> deniedColumnsByPrincipal = new LinkedHashMap<>();
        private final Map<String, Set<String>> allowedFunctionsByPrincipal = new LinkedHashMap<>();

        /**
         * Adds denied table key.
         *
         * @param tableKey table key in {@code table} or {@code schema.table} form.
         * @return this builder.
         */
        public Builder denyTable(String tableKey) {
            deniedTables.add(Objects.requireNonNull(tableKey, "tableKey"));
            return this;
        }

        /**
         * Adds denied column key.
         *
         * @param columnKey column key in {@code column} or {@code source.column} form.
         * @return this builder.
         */
        public Builder denyColumn(String columnKey) {
            deniedColumns.add(Objects.requireNonNull(columnKey, "columnKey"));
            return this;
        }

        /**
         * Adds allowed function name.
         *
         * @param functionName function name.
         * @return this builder.
         */
        public Builder allowFunction(String functionName) {
            allowedFunctions.add(Objects.requireNonNull(functionName, "functionName"));
            return this;
        }

        /**
         * Adds denied table key for a principal.
         *
         * @param principal principal identifier.
         * @param tableKey table key in {@code table} or {@code schema.table} form.
         * @return this builder.
         */
        public Builder denyTableForPrincipal(String principal, String tableKey) {
            rulesFor(deniedTablesByPrincipal, principal).add(Objects.requireNonNull(tableKey, "tableKey"));
            return this;
        }

        /**
         * Adds denied column key for a principal.
         *
         * @param principal principal identifier.
         * @param columnKey column key in {@code column} or {@code source.column} form.
         * @return this builder.
         */
        public Builder denyColumnForPrincipal(String principal, String columnKey) {
            rulesFor(deniedColumnsByPrincipal, principal).add(Objects.requireNonNull(columnKey, "columnKey"));
            return this;
        }

        /**
         * Adds allowed function name for a principal.
         *
         * @param principal principal identifier.
         * @param functionName function name.
         * @return this builder.
         */
        public Builder allowFunctionForPrincipal(String principal, String functionName) {
            rulesFor(allowedFunctionsByPrincipal, principal).add(Objects.requireNonNull(functionName, "functionName"));
            return this;
        }

        /**
         * Builds immutable policy.
         *
         * @return policy instance.
         */
        public DefaultCatalogAccessPolicy build() {
            return new DefaultCatalogAccessPolicy(
                deniedTables,
                deniedColumns,
                allowedFunctions,
                deniedTablesByPrincipal,
                deniedColumnsByPrincipal,
                allowedFunctionsByPrincipal
            );
        }

        private static Set<String> rulesFor(Map<String, Set<String>> rulesByPrincipal, String principal) {
            Objects.requireNonNull(principal, "principal");
            return rulesByPrincipal.computeIfAbsent(principal, key -> new LinkedHashSet<>());
        }
    }
}

