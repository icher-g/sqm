package io.sqm.catalog.access;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Default catalog access policy with optional principal- and tenant-specific overrides.
 *
 * <p>All keys are matched case-insensitively.</p>
 */
public final class DefaultCatalogAccessPolicy implements CatalogAccessPolicy {
    private static final String ANY_SCOPE = "*";
    private static final String SEPARATOR = "|";
    private static final DefaultCatalogAccessPolicy ALLOW_ALL = builder().build();

    private final Map<String, Set<String>> deniedTablesByScope;
    private final Map<String, Set<String>> deniedColumnsByScope;
    private final Map<String, Set<String>> allowedFunctionsByScope;

    private DefaultCatalogAccessPolicy(
        Map<String, Set<String>> deniedTablesByScope,
        Map<String, Set<String>> deniedColumnsByScope,
        Map<String, Set<String>> allowedFunctionsByScope
    ) {
        this.deniedTablesByScope = immutableScopeRules(deniedTablesByScope);
        this.deniedColumnsByScope = immutableScopeRules(deniedColumnsByScope);
        this.allowedFunctionsByScope = immutableScopeRules(allowedFunctionsByScope);
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
        return isTableDenied(null, principal, schemaName, tableName);
    }

    /**
     * Returns true when a table is denied for the tenant and principal.
     *
     * @param tenant tenant identifier, may be {@code null}.
     * @param principal principal identifier, may be {@code null}.
     * @param schemaName table schema, may be {@code null}.
     * @param tableName table name.
     * @return true when denied.
     */
    @Override
    public boolean isTableDenied(String tenant, String principal, String schemaName, String tableName) {
        if (tableName == null) {
            return false;
        }
        var table = normalize(tableName);
        for (var scope : scopesInPrecedence(tenant, principal)) {
            if (containsTable(deniedTablesByScope.getOrDefault(scope, Set.of()), schemaName, table)) {
                return true;
            }
        }
        return false;
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
        return isColumnDenied(null, principal, sourceName, columnName);
    }

    /**
     * Returns true when a column is denied for the tenant and principal.
     *
     * @param tenant tenant identifier, may be {@code null}.
     * @param principal principal identifier, may be {@code null}.
     * @param sourceName source alias or table name, may be {@code null}.
     * @param columnName column name.
     * @return true when denied.
     */
    @Override
    public boolean isColumnDenied(String tenant, String principal, String sourceName, String columnName) {
        if (columnName == null) {
            return false;
        }
        var column = normalize(columnName);
        for (var scope : scopesInPrecedence(tenant, principal)) {
            if (containsColumn(deniedColumnsByScope.getOrDefault(scope, Set.of()), sourceName, column)) {
                return true;
            }
        }
        return false;
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
        return isFunctionAllowed(null, principal, functionName);
    }

    /**
     * Returns true when a function is allowed for the tenant and principal.
     *
     * @param tenant tenant identifier, may be {@code null}.
     * @param principal principal identifier, may be {@code null}.
     * @param functionName function name.
     * @return true when allowed.
     */
    @Override
    public boolean isFunctionAllowed(String tenant, String principal, String functionName) {
        if (functionName == null) {
            return true;
        }
        if (allowedFunctionsByScope.values().stream().allMatch(Set::isEmpty)) {
            return true;
        }
        var normalized = normalize(functionName);
        for (var scope : scopesInPrecedence(tenant, principal)) {
            if (allowedFunctionsByScope.getOrDefault(scope, Set.of()).contains(normalized)) {
                return true;
            }
        }
        return false;
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

    private static java.util.List<String> scopesInPrecedence(String tenant, String principal) {
        return java.util.List.of(
            scopeKey(null, null),
            scopeKey(null, principal),
            scopeKey(tenant, null),
            scopeKey(tenant, principal)
        );
    }

    private static Map<String, Set<String>> immutableScopeRules(Map<String, Set<String>> map) {
        Objects.requireNonNull(map, "map");
        var immutable = new LinkedHashMap<String, Set<String>>(map.size());
        for (var entry : map.entrySet()) {
            immutable.put(entry.getKey(), Set.copyOf(entry.getValue()));
        }
        return Map.copyOf(immutable);
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private static String scopeKey(String tenant, String principal) {
        var normalizedTenant = tenant == null || tenant.isBlank() ? ANY_SCOPE : normalize(tenant);
        var normalizedPrincipal = principal == null || principal.isBlank() ? ANY_SCOPE : normalize(principal);
        return normalizedTenant + SEPARATOR + normalizedPrincipal;
    }

    /**
     * Mutable builder for {@link DefaultCatalogAccessPolicy}.
     */
    public static final class Builder {
        private final Map<String, Set<String>> deniedTablesByScope = new LinkedHashMap<>();
        private final Map<String, Set<String>> deniedColumnsByScope = new LinkedHashMap<>();
        private final Map<String, Set<String>> allowedFunctionsByScope = new LinkedHashMap<>();

        /**
         * Creates an empty access-policy builder.
         */
        public Builder() {
        }

        /**
         * Adds denied table key.
         *
         * @param tableKey table key in {@code table} or {@code schema.table} form.
         * @return this builder.
         */
        public Builder denyTable(String tableKey) {
            addRule(deniedTablesByScope, scopeKey(null, null), tableKey);
            return this;
        }

        /**
         * Adds denied column key.
         *
         * @param columnKey column key in {@code column} or {@code source.column} form.
         * @return this builder.
         */
        public Builder denyColumn(String columnKey) {
            addRule(deniedColumnsByScope, scopeKey(null, null), columnKey);
            return this;
        }

        /**
         * Adds allowed function name.
         *
         * @param functionName function name.
         * @return this builder.
         */
        public Builder allowFunction(String functionName) {
            addRule(allowedFunctionsByScope, scopeKey(null, null), functionName);
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
            addRule(deniedTablesByScope, principalScope(principal), tableKey);
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
            addRule(deniedColumnsByScope, principalScope(principal), columnKey);
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
            addRule(allowedFunctionsByScope, principalScope(principal), functionName);
            return this;
        }

        /**
         * Adds denied table key for a tenant.
         *
         * @param tenant tenant identifier.
         * @param tableKey table key in {@code table} or {@code schema.table} form.
         * @return this builder.
         */
        public Builder denyTableForTenant(String tenant, String tableKey) {
            addRule(deniedTablesByScope, tenantScope(tenant), tableKey);
            return this;
        }

        /**
         * Adds denied column key for a tenant.
         *
         * @param tenant tenant identifier.
         * @param columnKey column key in {@code column} or {@code source.column} form.
         * @return this builder.
         */
        public Builder denyColumnForTenant(String tenant, String columnKey) {
            addRule(deniedColumnsByScope, tenantScope(tenant), columnKey);
            return this;
        }

        /**
         * Adds allowed function name for a tenant.
         *
         * @param tenant tenant identifier.
         * @param functionName function name.
         * @return this builder.
         */
        public Builder allowFunctionForTenant(String tenant, String functionName) {
            addRule(allowedFunctionsByScope, tenantScope(tenant), functionName);
            return this;
        }

        /**
         * Adds denied table key for a tenant and principal.
         *
         * @param tenant tenant identifier.
         * @param principal principal identifier.
         * @param tableKey table key in {@code table} or {@code schema.table} form.
         * @return this builder.
         */
        public Builder denyTableForTenantPrincipal(String tenant, String principal, String tableKey) {
            addRule(deniedTablesByScope, tenantPrincipalScope(tenant, principal), tableKey);
            return this;
        }

        /**
         * Adds denied column key for a tenant and principal.
         *
         * @param tenant tenant identifier.
         * @param principal principal identifier.
         * @param columnKey column key in {@code column} or {@code source.column} form.
         * @return this builder.
         */
        public Builder denyColumnForTenantPrincipal(String tenant, String principal, String columnKey) {
            addRule(deniedColumnsByScope, tenantPrincipalScope(tenant, principal), columnKey);
            return this;
        }

        /**
         * Adds allowed function name for a tenant and principal.
         *
         * @param tenant tenant identifier.
         * @param principal principal identifier.
         * @param functionName function name.
         * @return this builder.
         */
        public Builder allowFunctionForTenantPrincipal(String tenant, String principal, String functionName) {
            addRule(allowedFunctionsByScope, tenantPrincipalScope(tenant, principal), functionName);
            return this;
        }

        /**
         * Builds immutable policy.
         *
         * @return policy instance.
         */
        public DefaultCatalogAccessPolicy build() {
            return new DefaultCatalogAccessPolicy(
                deniedTablesByScope,
                deniedColumnsByScope,
                allowedFunctionsByScope
            );
        }

        private static void addRule(Map<String, Set<String>> rulesByScope, String scope, String rule) {
            Objects.requireNonNull(scope, "scope");
            Objects.requireNonNull(rule, "rule");
            if (rule.isBlank()) {
                return;
            }
            rulesByScope.computeIfAbsent(scope, ignored -> new LinkedHashSet<>()).add(normalize(rule));
        }

        private static String principalScope(String principal) {
            if (principal == null || principal.isBlank()) {
                throw new IllegalArgumentException("principal must not be blank");
            }
            return scopeKey(null, principal);
        }

        private static String tenantScope(String tenant) {
            if (tenant == null || tenant.isBlank()) {
                throw new IllegalArgumentException("tenant must not be blank");
            }
            return scopeKey(tenant, null);
        }

        private static String tenantPrincipalScope(String tenant, String principal) {
            if (tenant == null || tenant.isBlank()) {
                throw new IllegalArgumentException("tenant must not be blank");
            }
            if (principal == null || principal.isBlank()) {
                throw new IllegalArgumentException("principal must not be blank");
            }
            return scopeKey(tenant, principal);
        }
    }
}
