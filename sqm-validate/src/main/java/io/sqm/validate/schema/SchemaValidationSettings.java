package io.sqm.validate.schema;

import io.sqm.core.Node;
import io.sqm.catalog.access.CatalogAccessPolicies;
import io.sqm.catalog.access.CatalogAccessPolicy;
import io.sqm.validate.schema.function.DefaultFunctionCatalog;
import io.sqm.validate.schema.function.FunctionCatalog;
import io.sqm.validate.schema.rule.SchemaValidationRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Configuration object for schema-query validation.
 *
 * <p>This settings object keeps extension points stable while preserving
 * default validation behavior unless explicitly customized.</p>
 */
public final class SchemaValidationSettings {
    private final FunctionCatalog functionCatalog;
    private final CatalogAccessPolicy accessPolicy;
    private final String principal;
    private final SchemaValidationLimits limits;
    private final List<SchemaValidationRule<? extends Node>> additionalRules;

    private SchemaValidationSettings(
        FunctionCatalog functionCatalog,
        CatalogAccessPolicy accessPolicy,
        String principal,
        SchemaValidationLimits limits,
        List<SchemaValidationRule<? extends Node>> additionalRules
    ) {
        this.functionCatalog = Objects.requireNonNull(functionCatalog, "functionCatalog");
        this.accessPolicy = Objects.requireNonNull(accessPolicy, "accessPolicy");
        this.principal = normalizePrincipal(principal);
        this.limits = Objects.requireNonNull(limits, "limits");
        this.additionalRules = List.copyOf(additionalRules);
    }

    /**
     * Creates default settings with the built-in function catalog and no extra rules.
     *
     * @return default settings.
     */
    public static SchemaValidationSettings defaults() {
        return builder().build();
    }

    /**
     * Creates settings with an explicit function catalog and no extra rules.
     *
     * @param functionCatalog function catalog used during validation.
     * @return settings instance.
     */
    public static SchemaValidationSettings of(FunctionCatalog functionCatalog) {
        return builder().functionCatalog(functionCatalog).build();
    }

    /**
     * Creates a mutable builder.
     *
     * @return settings builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the effective function catalog.
     *
     * @return function catalog.
     */
    public FunctionCatalog functionCatalog() {
        return functionCatalog;
    }

    /**
     * Returns access policy used by policy-aware validation rules.
     *
     * @return schema access policy.
     */
    public CatalogAccessPolicy accessPolicy() {
        return accessPolicy;
    }

    /**
     * Returns principal identifier used for principal-aware access checks.
     *
     * @return principal identifier, may be {@code null}.
     */
    public String principal() {
        return principal;
    }

    /**
     * Returns configured structural validation limits.
     */
    public SchemaValidationLimits limits() {
        return limits;
    }

    /**
     * Returns additional node rules appended after default rules.
     *
     * @return immutable list of additional rules.
     */
    public List<SchemaValidationRule<? extends Node>> additionalRules() {
        return additionalRules;
    }

    /**
     * Mutable builder for {@link SchemaValidationSettings}.
     */
    public static final class Builder {
        private FunctionCatalog functionCatalog = DefaultFunctionCatalog.standard();
        private CatalogAccessPolicy accessPolicy = CatalogAccessPolicies.allowAll();
        private String principal;
        private SchemaValidationLimits limits = SchemaValidationLimits.unlimited();
        private final List<SchemaValidationRule<? extends Node>> additionalRules = new ArrayList<>();

        /**
         * Sets function catalog used by function-signature and aggregation rules.
         *
         * @param functionCatalog function catalog.
         * @return this builder.
         */
        public Builder functionCatalog(FunctionCatalog functionCatalog) {
            this.functionCatalog = Objects.requireNonNull(functionCatalog, "functionCatalog");
            return this;
        }

        /**
         * Sets schema access policy used by table/column/function access checks.
         *
         * @param accessPolicy schema access policy.
         * @return this builder.
         */
        public Builder accessPolicy(CatalogAccessPolicy accessPolicy) {
            this.accessPolicy = Objects.requireNonNull(accessPolicy, "accessPolicy");
            return this;
        }

        /**
         * Sets principal identifier used for principal-aware access checks.
         *
         * @param principal principal identifier, may be {@code null}.
         * @return this builder.
         */
        public Builder principal(String principal) {
            this.principal = normalizePrincipal(principal);
            return this;
        }

        /**
         * Sets structural validation limits.
         *
         * @param limits structural limits.
         * @return this builder.
         */
        public Builder limits(SchemaValidationLimits limits) {
            this.limits = Objects.requireNonNull(limits, "limits");
            return this;
        }

        /**
         * Adds an extra schema-validation rule.
         *
         * @param rule rule to append after default rules.
         * @return this builder.
         */
        public Builder addRule(SchemaValidationRule<? extends Node> rule) {
            this.additionalRules.add(Objects.requireNonNull(rule, "rule"));
            return this;
        }

        /**
         * Adds extra schema-validation rules.
         *
         * @param rules rules to append after default rules.
         * @return this builder.
         */
        public Builder addRules(List<SchemaValidationRule<? extends Node>> rules) {
            Objects.requireNonNull(rules, "rules");
            for (var rule : rules) {
                addRule(rule);
            }
            return this;
        }

        /**
         * Builds immutable settings.
         *
         * @return settings instance.
         */
        public SchemaValidationSettings build() {
            return new SchemaValidationSettings(
                functionCatalog,
                accessPolicy,
                principal,
                limits,
                additionalRules
            );
        }
    }

    private static String normalizePrincipal(String principal) {
        if (principal == null || principal.isBlank()) {
            return null;
        }
        return principal;
    }
}
