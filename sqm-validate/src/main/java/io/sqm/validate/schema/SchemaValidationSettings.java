package io.sqm.validate.schema;

import io.sqm.core.Node;
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
    private final List<SchemaValidationRule<? extends Node>> additionalRules;

    private SchemaValidationSettings(
        FunctionCatalog functionCatalog,
        List<SchemaValidationRule<? extends Node>> additionalRules
    ) {
        this.functionCatalog = Objects.requireNonNull(functionCatalog, "functionCatalog");
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
            return new SchemaValidationSettings(functionCatalog, additionalRules);
        }
    }
}
