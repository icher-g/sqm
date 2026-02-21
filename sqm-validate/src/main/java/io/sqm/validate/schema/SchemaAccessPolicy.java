package io.sqm.validate.schema;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Access policy used by schema validator for table/column/function restrictions.
 *
 * <p>All keys are matched case-insensitively.</p>
 */
public final class SchemaAccessPolicy {
    private final Set<String> deniedTables;
    private final Set<String> deniedColumns;
    private final Set<String> allowedFunctions;

    private SchemaAccessPolicy(Set<String> deniedTables, Set<String> deniedColumns, Set<String> allowedFunctions) {
        this.deniedTables = normalizeAll(deniedTables);
        this.deniedColumns = normalizeAll(deniedColumns);
        this.allowedFunctions = normalizeAll(allowedFunctions);
    }

    /**
     * Creates a policy with no restrictions.
     *
     * @return allow-all policy.
     */
    public static SchemaAccessPolicy allowAll() {
        return new SchemaAccessPolicy(Set.of(), Set.of(), Set.of());
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
     * Returns true when a table is denied.
     *
     * <p>Matches either {@code table} or {@code schema.table} keys.</p>
     *
     * @param schemaName table schema, may be {@code null}.
     * @param tableName table name.
     * @return true when denied.
     */
    public boolean isTableDenied(String schemaName, String tableName) {
        if (tableName == null) {
            return false;
        }
        var table = normalize(tableName);
        if (deniedTables.contains(table)) {
            return true;
        }
        if (schemaName == null || schemaName.isBlank()) {
            return false;
        }
        return deniedTables.contains(normalize(schemaName) + "." + table);
    }

    /**
     * Returns true when a column is denied.
     *
     * <p>Matches either {@code column} or {@code source.column} keys.</p>
     *
     * @param sourceName source/table alias or table name, may be {@code null}.
     * @param columnName column name.
     * @return true when denied.
     */
    public boolean isColumnDenied(String sourceName, String columnName) {
        if (columnName == null) {
            return false;
        }
        var column = normalize(columnName);
        if (deniedColumns.contains(column)) {
            return true;
        }
        if (sourceName == null || sourceName.isBlank()) {
            return false;
        }
        return deniedColumns.contains(normalize(sourceName) + "." + column);
    }

    /**
     * Returns true when a function is allowed.
     *
     * <p>If allowlist is empty, all functions are allowed.</p>
     *
     * @param functionName function name.
     * @return true when allowed.
     */
    public boolean isFunctionAllowed(String functionName) {
        if (functionName == null) {
            return true;
        }
        if (allowedFunctions.isEmpty()) {
            return true;
        }
        return allowedFunctions.contains(normalize(functionName));
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
     * Mutable builder for {@link SchemaAccessPolicy}.
     */
    public static final class Builder {
        private final Set<String> deniedTables = new LinkedHashSet<>();
        private final Set<String> deniedColumns = new LinkedHashSet<>();
        private final Set<String> allowedFunctions = new LinkedHashSet<>();

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
         * Builds immutable policy.
         *
         * @return policy instance.
         */
        public SchemaAccessPolicy build() {
            return new SchemaAccessPolicy(deniedTables, deniedColumns, allowedFunctions);
        }
    }
}
