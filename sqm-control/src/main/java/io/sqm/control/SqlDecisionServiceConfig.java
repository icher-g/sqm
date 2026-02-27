package io.sqm.control;

import io.sqm.catalog.model.CatalogSchema;
import io.sqm.validate.schema.SchemaValidationSettings;
import io.sqm.validate.schema.SchemaValidationSettingsLoader;

import java.util.Objects;
import java.util.Set;

/**
 * Immutable configuration object for assembling {@link SqlDecisionService} instances.
 *
 * <p>This type provides named factory methods for common middleware flows and fluent
 * customization methods for optional concerns (audit, explain, parser, guardrails),
 * avoiding large overload sets on {@link SqlDecisionService}.</p>
 *
 * @param engine decision engine used to evaluate parsed queries.
 * @param explainer decision explainer used to enrich audit output.
 * @param auditPublisher audit event publisher implementation.
 * @param guardrails runtime guardrails applied before execution.
 * @param queryParser SQL parser used to parse incoming query text.
 */
public record SqlDecisionServiceConfig(
    SqlDecisionEngine engine,
    SqlDecisionExplainer explainer,
    AuditEventPublisher auditPublisher,
    RuntimeGuardrails guardrails,
    SqlQueryParser queryParser) {

    /**
     * Validates required configuration fields.
     *
     * @param engine         decision engine
     * @param explainer      explanation strategy
     * @param auditPublisher audit publisher
     * @param guardrails     runtime guardrails
     * @param queryParser    SQL parser
     */
    public SqlDecisionServiceConfig {
        Objects.requireNonNull(engine, "engine must not be null");
        Objects.requireNonNull(queryParser, "queryParser must not be null");
    }

    /**
     * Creates a builder pre-bound to the provided catalog schema.
     *
     * @param schema catalog schema used by validation and schema-aware rewrites
     * @return middleware config builder
     */
    public static Builder builder(CatalogSchema schema) {
        return new Builder(schema);
    }

    /**
     * A builder class for composing {@link SqlDecisionServiceConfig}.
     */
    public static final class Builder {
        private final CatalogSchema schema;
        private SchemaValidationSettings validationSettings;
        private String validationSettingsJson;
        private String validationSettingsYaml;
        private BuiltInRewriteSettings rewriteSettings;
        private Set<BuiltInRewriteRule> rewriteRules;
        private SqlDecisionExplainer explainer;
        private AuditEventPublisher eventPublisher;
        private RuntimeGuardrails guardrails;
        private SqlQueryParser queryParser;
        private SqlQueryValidator queryValidator;
        private SqlQueryRenderer queryRenderer;
        private SqlQueryRewriter queryRewriter;

        /**
         * Creates a builder bound to a catalog schema.
         *
         * @param schema catalog schema
         */
        public Builder(CatalogSchema schema) {
            this.schema = Objects.requireNonNull(schema, "schema must not be null");
        }

        /**
         * Sets schema-validation settings used by the query validator.
         *
         * @param validationSettings schema-validation settings
         * @return builder instance
         */
        public Builder validationSettings(SchemaValidationSettings validationSettings) {
            this.validationSettings = validationSettings;
            return this;
        }

        /**
         * Sets schema-validation settings in JSON format.
         *
         * <p>When explicit {@link #validationSettings(SchemaValidationSettings)} is not provided,
         * this configuration text is loaded via {@link SchemaValidationSettingsLoader#fromJson(String)}
         * and used to create validation settings (including access policy).</p>
         *
         * @param validationSettingsJson schema-validation settings JSON
         * @return builder instance
         */
        public Builder validationSettingsJson(String validationSettingsJson) {
            this.validationSettingsJson = validationSettingsJson;
            return this;
        }

        /**
         * Sets schema-validation settings in YAML format.
         *
         * <p>When explicit {@link #validationSettings(SchemaValidationSettings)} is not provided,
         * this configuration text is loaded via {@link SchemaValidationSettingsLoader#fromYaml(String)}
         * and used to create validation settings (including access policy).</p>
         *
         * @param validationSettingsYaml schema-validation settings YAML
         * @return builder instance
         */
        public Builder validationSettingsYaml(String validationSettingsYaml) {
            this.validationSettingsYaml = validationSettingsYaml;
            return this;
        }

        /**
         * Sets built-in rewrite rules for rewrite-enabled configurations.
         *
         * @param rewriteRules rewrite rules to enable
         * @return builder instance
         */
        public Builder rewriteRules(BuiltInRewriteRule... rewriteRules) {
            this.rewriteRules = Set.of(rewriteRules);
            return this;
        }

        /**
         * Sets built-in rewrite settings.
         *
         * @param rewriteSettings rewrite settings
         * @return builder instance
         */
        public Builder builtInRewriteSettings(BuiltInRewriteSettings rewriteSettings) {
            this.rewriteSettings = rewriteSettings;
            return this;
        }

        /**
         * Sets custom decision explainer.
         *
         * @param explainer explanation strategy
         * @return builder instance
         */
        public Builder explainer(SqlDecisionExplainer explainer) {
            this.explainer = explainer;
            return this;
        }

        /**
         * Sets custom audit publisher.
         *
         * @param eventPublisher audit publisher
         * @return builder instance
         */
        public Builder auditPublisher(AuditEventPublisher eventPublisher) {
            this.eventPublisher = eventPublisher;
            return this;
        }

        /**
         * Sets runtime guardrails.
         *
         * @param guardrails runtime guardrails
         * @return builder instance
         */
        public Builder guardrails(RuntimeGuardrails guardrails) {
            this.guardrails = guardrails;
            return this;
        }

        /**
         * Sets custom SQL parser.
         *
         * @param queryParser SQL parser
         * @return builder instance
         */
        public Builder queryParser(SqlQueryParser queryParser) {
            this.queryParser = queryParser;
            return this;
        }

        /**
         * Sets custom query validator.
         *
         * @param queryValidator query validator
         * @return builder instance
         */
        public Builder queryValidator(SqlQueryValidator queryValidator) {
            this.queryValidator = queryValidator;
            return this;
        }

        /**
         * Sets custom query renderer.
         *
         * @param queryRenderer query renderer
         * @return builder instance
         */
        public Builder queryRenderer(SqlQueryRenderer queryRenderer) {
            this.queryRenderer = queryRenderer;
            return this;
        }

        /**
         * Sets custom query rewriter.
         *
         * @param sqlQueryRewriter query rewriter
         * @return builder instance
         */
        public Builder queryRewriter(SqlQueryRewriter sqlQueryRewriter) {
            this.queryRewriter = sqlQueryRewriter;
            return this;
        }

        /**
         * Builds validation-only configuration with defaults for omitted optional components.
         *
         * @return validation-only middleware config
         */
        public SqlDecisionServiceConfig buildValidationConfig() {
            if (queryParser == null) {
                queryParser = SqlQueryParser.standard();
            }

            if (queryValidator == null) {
                queryValidator = SqlQueryValidator.standard(schema, resolveValidationSettings());
            }

            if (queryRenderer == null) {
                queryRenderer = (query, context) -> {
                    throw new IllegalStateException("queryRenderer must be configured when rewrite rules are enabled");
                };
            }

            if (queryRewriter == null) {
                queryRewriter = SqlQueryRewriter.noop();
            }

            if (guardrails == null) {
                guardrails = RuntimeGuardrails.disabled();
            }

            if (eventPublisher == null) {
                eventPublisher = AuditEventPublisher.noop();
            }

            if (explainer == null) {
                explainer = SqlDecisionExplainer.basic();
            }

            return new SqlDecisionServiceConfig(
                SqlDecisionEngine.of(queryValidator, queryRewriter, queryRenderer),
                explainer,
                eventPublisher,
                guardrails,
                queryParser
            );
        }

        /**
         * Builds validation+rewrite configuration with defaults for omitted optional components.
         *
         * <p>When rewrite settings are not explicitly configured, {@link BuiltInRewriteSettings#defaults()} is used.
         * When rewrite rules are not explicitly configured, all currently available schema-aware built-in rewrite
         * rules are enabled by default.
         * If rewrite rules are explicitly configured as empty, this method falls back to
         * {@link #buildValidationConfig()} semantics.</p>
         *
         * @return validation+rewrite middleware config
         */
        public SqlDecisionServiceConfig buildValidationAndRewriteConfig() {
            if (queryParser == null) {
                queryParser = SqlQueryParser.standard();
            }

            if (queryValidator == null) {
                queryValidator = SqlQueryValidator.standard(schema, resolveValidationSettings());
            }

            if (queryRenderer == null) {
                queryRenderer = SqlQueryRenderer.standard();
            }

            if (queryRewriter == null) {
                if (rewriteSettings == null) {
                    rewriteSettings = BuiltInRewriteSettings.defaults();
                }

                if (rewriteRules == null) {
                    rewriteRules = Set.of();
                }

                queryRewriter = SqlQueryRewriter.builder()
                    .schema(schema)
                    .settings(rewriteSettings)
                    .rules(rewriteRules)
                    .build();
            }

            if (guardrails == null) {
                guardrails = RuntimeGuardrails.disabled();
            }

            if (eventPublisher == null) {
                eventPublisher = AuditEventPublisher.noop();
            }

            if (explainer == null) {
                explainer = SqlDecisionExplainer.basic();
            }

            return new SqlDecisionServiceConfig(
                SqlDecisionEngine.of(queryValidator, queryRewriter, queryRenderer),
                explainer,
                eventPublisher,
                guardrails,
                queryParser
            );
        }

        private SchemaValidationSettings resolveValidationSettings() {
            if (validationSettings != null) {
                return validationSettings;
            }
            if (validationSettingsJson != null && !validationSettingsJson.isBlank()) {
                return SchemaValidationSettingsLoader.fromJson(validationSettingsJson);
            }
            if (validationSettingsYaml != null && !validationSettingsYaml.isBlank()) {
                return SchemaValidationSettingsLoader.fromYaml(validationSettingsYaml);
            }

            String json = firstNonBlank(
                System.getProperty(ConfigKeys.VALIDATION_SETTINGS_JSON.property()),
                System.getenv(ConfigKeys.VALIDATION_SETTINGS_JSON.env())
            );
            if (json != null) {
                return SchemaValidationSettingsLoader.fromJson(json);
            }

            String yaml = firstNonBlank(
                System.getProperty(ConfigKeys.VALIDATION_SETTINGS_YAML.property()),
                System.getenv(ConfigKeys.VALIDATION_SETTINGS_YAML.env())
            );
            if (yaml != null) {
                return SchemaValidationSettingsLoader.fromYaml(yaml);
            }

            return SchemaValidationSettings.defaults();
        }

        private static String firstNonBlank(String first, String second) {
            if (first != null && !first.isBlank()) {
                return first;
            }
            if (second != null && !second.isBlank()) {
                return second;
            }
            return null;
        }
    }
}

