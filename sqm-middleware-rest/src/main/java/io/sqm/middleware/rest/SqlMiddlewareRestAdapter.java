package io.sqm.middleware.rest;

import io.sqm.middleware.api.AnalyzeRequest;
import io.sqm.middleware.api.DecisionExplanationDto;
import io.sqm.middleware.api.DecisionResultDto;
import io.sqm.middleware.api.EnforceRequest;
import io.sqm.middleware.api.ExplainRequest;
import io.sqm.middleware.api.SqlMiddlewareService;

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
        return service.analyze(request);
    }

    /**
     * Handles REST enforce requests.
     *
     * @param request enforce request payload
     * @return decision payload
     */
    public DecisionResultDto enforce(EnforceRequest request) {
        return service.enforce(request);
    }

    /**
     * Handles REST explain requests.
     *
     * @param request explain request payload
     * @return explanation payload
     */
    public DecisionExplanationDto explain(ExplainRequest request) {
        return service.explainDecision(request);
    }
}
