package io.sqm.middleware.api;

/**
 * Transport-neutral SQL middleware service contract.
 */
public interface SqlMiddlewareService {

    /**
     * Analyzes SQL for an execution context.
     *
     * @param request analyze request payload
     * @return decision result payload
     */
    DecisionResultDto analyze(AnalyzeRequest request);

    /**
     * Enforces policies against SQL for an execution context.
     *
     * @param request enforce request payload
     * @return decision result payload
     */
    DecisionResultDto enforce(EnforceRequest request);

    /**
     * Produces a detailed explanation for the middleware decision.
     *
     * @param request explain request payload
     * @return decision explanation payload
     */
    DecisionExplanationDto explainDecision(ExplainRequest request);
}
