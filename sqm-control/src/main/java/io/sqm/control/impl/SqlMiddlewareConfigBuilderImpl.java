package io.sqm.control.impl;

import io.sqm.catalog.model.CatalogSchema;
import io.sqm.control.*;
import io.sqm.validate.schema.SchemaValidationSettings;

import java.util.Objects;
import java.util.Set;

/**
 * Default implementation of {@link SqlMiddlewareConfigBuilder}.
 */
public class SqlMiddlewareConfigBuilderImpl implements SqlMiddlewareConfigBuilder {

    private final CatalogSchema schema;
    private SchemaValidationSettings validationSettings;
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
    public SqlMiddlewareConfigBuilderImpl(CatalogSchema schema) {
        this.schema = Objects.requireNonNull(schema, "schema must not be null");
    }

    /** {@inheritDoc} */
    @Override
    public SqlMiddlewareConfigBuilder validationSettings(SchemaValidationSettings validationSettings) {
        this.validationSettings = validationSettings;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public SqlMiddlewareConfigBuilder rewriteRules(BuiltInRewriteRule... rewriteRules) {
        this.rewriteRules = Set.of(rewriteRules);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public SqlMiddlewareConfigBuilder builtInRewriteSettings(BuiltInRewriteSettings rewriteSettings) {
        this.rewriteSettings = rewriteSettings;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public SqlMiddlewareConfigBuilder explainer(SqlDecisionExplainer explainer) {
        this.explainer = explainer;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public SqlMiddlewareConfigBuilder auditPublisher(AuditEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public SqlMiddlewareConfigBuilder guardrails(RuntimeGuardrails guardrails) {
        this.guardrails = guardrails;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public SqlMiddlewareConfigBuilder queryParser(SqlQueryParser queryParser) {
        this.queryParser = queryParser;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public SqlMiddlewareConfigBuilder queryValidator(SqlQueryValidator queryValidator) {
        this.queryValidator = queryValidator;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public SqlMiddlewareConfigBuilder queryRenderer(SqlQueryRenderer queryRenderer) {
        this.queryRenderer = queryRenderer;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public SqlMiddlewareConfigBuilder queryRewriter(SqlQueryRewriter sqlQueryRewriter) {
        this.queryRewriter = sqlQueryRewriter;
        return this;
    }

    /**
     * Builds validation-only configuration with defaults for omitted optional components.
     *
     * @return validation-only middleware config
     */
    @Override
    public SqlMiddlewareConfig buildValidationConfig() {
        if (queryParser == null) {
            queryParser = SqlQueryParser.standard();
        }

        if (queryValidator == null) {
            if (validationSettings == null) {
                queryValidator = SqlQueryValidator.standard(schema);
            }
            else {
                queryValidator = SqlQueryValidator.standard(schema, validationSettings);
            }
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

        return new SqlMiddlewareConfig(
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
     * @return validation+rewrite middleware config
     */
    @Override
    public SqlMiddlewareConfig buildValidationAndRewriteConfig() {
        if (queryParser == null) {
            queryParser = SqlQueryParser.standard();
        }

        if (queryValidator == null) {
            if (validationSettings == null) {
                queryValidator = SqlQueryValidator.standard(schema);
            }
            else {
                queryValidator = SqlQueryValidator.standard(schema, validationSettings);
            }
        }

        if (queryRenderer == null) {
            queryRenderer = SqlQueryRenderer.standard();
        }

        if (queryRewriter == null) {
            queryRewriter = SqlQueryRewriter.builtIn(schema, rewriteSettings, rewriteRules);
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

        return new SqlMiddlewareConfig(
            SqlDecisionEngine.of(queryValidator, queryRewriter, queryRenderer),
            explainer,
            eventPublisher,
            guardrails,
            queryParser
        );
    }
}
