package io.sqm.control.config;

import io.sqm.catalog.model.CatalogSchema;
import io.sqm.control.audit.AuditEventPublisher;
import io.sqm.control.pipeline.SqlStatementParser;
import io.sqm.control.pipeline.SqlStatementRenderer;
import io.sqm.control.pipeline.SqlStatementRewriter;
import io.sqm.control.pipeline.SqlStatementValidator;
import io.sqm.control.rewrite.BuiltInRewriteRule;
import io.sqm.control.rewrite.BuiltInRewriteSettings;
import io.sqm.control.service.SqlDecisionEngine;
import io.sqm.control.service.SqlDecisionExplainer;
import io.sqm.control.service.SqlDecisionService;
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
 * @param engine         decision engine used to evaluate parsed statements.
 * @param explainer      decision explainer used to enrich audit result.
 * @param auditPublisher audit event publisher implementation.
 * @param guardrails     runtime guardrails applied before execution.
 * @param statementParser SQL parser used to parse incoming statement text.
 */
public record SqlDecisionServiceConfig(
    SqlDecisionEngine engine,
    SqlDecisionExplainer explainer,
    AuditEventPublisher auditPublisher,
    RuntimeGuardrails guardrails,
    SqlStatementParser statementParser) {

    /**
     * Validates required configuration fields.
     *
     * @param engine         decision engine
     * @param explainer      explanation strategy
     * @param auditPublisher audit publisher
     * @param guardrails     runtime guardrails
     * @param statementParser SQL parser
     */
    public SqlDecisionServiceConfig {
        Objects.requireNonNull(engine, "engine must not be null");
        Objects.requireNonNull(statementParser, "statementParser must not be null");
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
        private SqlStatementParser statementParser;
        private SqlStatementValidator statementValidator;
        private SqlStatementRenderer statementRenderer;
        private SqlStatementRewriter statementRewriter;

        /**
         * Creates a builder bound to a catalog schema.
         *
         * @param schema catalog schema
         */
        public Builder(CatalogSchema schema) {
            this.schema = Objects.requireNonNull(schema, "schema must not be null");
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
         * @param statementParser SQL parser
         * @return builder instance
         */
        public Builder statementParser(SqlStatementParser statementParser) {
            this.statementParser = statementParser;
            return this;
        }

        /**
         * Sets custom query validator.
         *
         * @param statementValidator statement validator
         * @return builder instance
         */
        public Builder statementValidator(SqlStatementValidator statementValidator) {
            this.statementValidator = statementValidator;
            return this;
        }

        /**
         * Sets custom query renderer.
         *
         * @param statementRenderer query renderer
         * @return builder instance
         */
        public Builder statementRenderer(SqlStatementRenderer statementRenderer) {
            this.statementRenderer = statementRenderer;
            return this;
        }

        /**
         * Sets custom statement rewriter.
         *
         * @param statementRewriter statement rewriter
         * @return builder instance
         */
        public Builder statementRewriter(SqlStatementRewriter statementRewriter) {
            this.statementRewriter = statementRewriter;
            return this;
        }

        /**
         * Builds validation-only configuration with defaults for omitted optional components.
         *
         * @return validation-only middleware config
         */
        public SqlDecisionServiceConfig buildValidationConfig() {
            if (statementParser == null) {
                statementParser = SqlStatementParser.standard();
            }

            if (statementValidator == null) {
                statementValidator = SqlStatementValidator.standard(schema, resolveValidationSettings());
            }

            if (statementRenderer == null) {
                statementRenderer = (query, context) -> {
                    throw new IllegalStateException("statementRenderer must be configured when rewrite rules are enabled");
                };
            }

            if (statementRewriter == null) {
                statementRewriter = SqlStatementRewriter.noop();
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
                SqlDecisionEngine.of(statementValidator, statementRewriter, statementRenderer),
                explainer,
                eventPublisher,
                guardrails,
                statementParser
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
            if (statementParser == null) {
                statementParser = SqlStatementParser.standard();
            }

            if (statementValidator == null) {
                statementValidator = SqlStatementValidator.standard(schema, resolveValidationSettings());
            }

            if (statementRenderer == null) {
                statementRenderer = SqlStatementRenderer.standard();
            }

            if (statementRewriter == null) {
                if (rewriteSettings == null) {
                    rewriteSettings = BuiltInRewriteSettings.defaults();
                }

                if (rewriteRules == null) {
                    rewriteRules = Set.of();
                }

                statementRewriter = SqlStatementRewriter.builder()
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
                SqlDecisionEngine.of(statementValidator, statementRewriter, statementRenderer),
                explainer,
                eventPublisher,
                guardrails,
                statementParser
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
    }
}