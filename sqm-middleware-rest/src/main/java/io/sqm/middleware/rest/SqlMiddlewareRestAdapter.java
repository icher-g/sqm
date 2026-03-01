package io.sqm.middleware.rest;

import io.sqm.middleware.api.*;

import java.util.Objects;

/**
 * Thin REST-facing adapter that delegates to {@link SqlMiddlewareService}.
 */
public final class SqlMiddlewareRestAdapter {

    private final SqlMiddlewareService service;

    /**
     * Creates an adapter backed by a middleware service.
     *
     * @param service middleware service
     */
    public SqlMiddlewareRestAdapter(SqlMiddlewareService service) {
        this.service = Objects.requireNonNull(service, "service must not be null");
    }

    /**
     * Handles REST analyze requests.
     *
     * @param request analyze request payload
     * @return decision payload
     */
    public DecisionResultDto analyze(AnalyzeRequest request) {
        validateAnalyzeRequest(request);
        return service.analyze(request);
    }

    /**
     * Handles REST enforce requests.
     *
     * @param request enforce request payload
     * @return decision payload
     */
    public DecisionResultDto enforce(EnforceRequest request) {
        validateEnforceRequest(request);
        return service.enforce(request);
    }

    /**
     * Handles REST explain requests.
     *
     * @param request explain request payload
     * @return explanation payload
     */
    public DecisionExplanationDto explain(ExplainRequest request) {
        validateExplainRequest(request);
        return service.explainDecision(request);
    }

    private static void validateAnalyzeRequest(AnalyzeRequest request) {
        if (request == null) {
            throw new InvalidRequestException("Request body must not be null");
        }
        validateCommon(request.sql(), request.context());
    }

    private static void validateEnforceRequest(EnforceRequest request) {
        if (request == null) {
            throw new InvalidRequestException("Request body must not be null");
        }
        validateCommon(request.sql(), request.context());
    }

    private static void validateExplainRequest(ExplainRequest request) {
        if (request == null) {
            throw new InvalidRequestException("Request body must not be null");
        }
        validateCommon(request.sql(), request.context());
    }

    private static void validateCommon(String sql, ExecutionContextDto context) {
        if (sql == null || sql.isBlank()) {
            throw new InvalidRequestException("Field 'sql' must not be blank");
        }
        if (context == null) {
            throw new InvalidRequestException("Field 'context' must not be null");
        }
        if (context.dialect() == null || context.dialect().isBlank()) {
            throw new InvalidRequestException("Field 'context.dialect' must not be blank");
        }
    }
}
