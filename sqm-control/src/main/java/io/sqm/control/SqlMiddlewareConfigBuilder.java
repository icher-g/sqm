package io.sqm.control;

import io.sqm.validate.schema.SchemaValidationSettings;

/**
 * Fluent builder for {@link SqlMiddlewareConfig} presets.
 */
public interface SqlMiddlewareConfigBuilder {

    /**
     * Sets schema-validation settings used by the query validator.
     *
     * @param validationSettings schema-validation settings
     * @return builder instance
     */
    SqlMiddlewareConfigBuilder validationSettings(SchemaValidationSettings validationSettings);

    /**
     * Sets built-in rewrite rules for rewrite-enabled configurations.
     *
     * @param rewriteRules rewrite rules to enable
     * @return builder instance
     */
    SqlMiddlewareConfigBuilder rewriteRules(BuiltInRewriteRule... rewriteRules);

    /**
     * Sets built-in rewrite settings.
     *
     * @param rewriteSettings rewrite settings
     * @return builder instance
     */
    SqlMiddlewareConfigBuilder builtInRewriteSettings(BuiltInRewriteSettings rewriteSettings);

    /**
     * Sets custom decision explainer.
     *
     * @param explainer explanation strategy
     * @return builder instance
     */
    SqlMiddlewareConfigBuilder explainer(SqlDecisionExplainer explainer);

    /**
     * Sets custom audit publisher.
     *
     * @param eventPublisher audit publisher
     * @return builder instance
     */
    SqlMiddlewareConfigBuilder auditPublisher(AuditEventPublisher eventPublisher);

    /**
     * Sets runtime guardrails.
     *
     * @param guardrails runtime guardrails
     * @return builder instance
     */
    SqlMiddlewareConfigBuilder guardrails(RuntimeGuardrails guardrails);

    /**
     * Sets custom SQL parser.
     *
     * @param queryParser SQL parser
     * @return builder instance
     */
    SqlMiddlewareConfigBuilder queryParser(SqlQueryParser queryParser);

    /**
     * Sets custom query validator.
     *
     * @param queryValidator query validator
     * @return builder instance
     */
    SqlMiddlewareConfigBuilder queryValidator(SqlQueryValidator queryValidator);

    /**
     * Sets custom query renderer.
     *
     * @param queryRenderer query renderer
     * @return builder instance
     */
    SqlMiddlewareConfigBuilder queryRenderer(SqlQueryRenderer queryRenderer);

    /**
     * Sets custom query rewriter.
     *
     * @param sqlQueryRewriter query rewriter
     * @return builder instance
     */
    SqlMiddlewareConfigBuilder queryRewriter(SqlQueryRewriter sqlQueryRewriter);

    /**
     * Builds validation-only middleware configuration.
     *
     * @return validation-only config
     */
    SqlMiddlewareConfig buildValidationConfig();

    /**
     * Builds validation+rewrite middleware configuration.
     *
     * @return validation+rewrite config
     */
    SqlMiddlewareConfig buildValidationAndRewriteConfig();
}
