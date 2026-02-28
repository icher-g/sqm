package io.sqm.middleware.core;

import io.sqm.middleware.api.AnalyzeRequest;
import io.sqm.middleware.api.DecisionExplanationDto;
import io.sqm.middleware.api.DecisionResultDto;
import io.sqm.middleware.api.EnforceRequest;
import io.sqm.middleware.api.ExplainRequest;
import io.sqm.middleware.api.SqlMiddlewareService;

import java.util.Objects;

/**
 * {@link SqlMiddlewareService} decorator that records telemetry for each operation.
 */
public final class ObservedSqlMiddlewareService implements SqlMiddlewareService {

    private final SqlMiddlewareService delegate;
    private final MiddlewareTelemetry telemetry;

    /**
     * Creates a telemetry-observed middleware service.
     *
     * @param delegate wrapped middleware service
     * @param telemetry telemetry recorder
     */
    public ObservedSqlMiddlewareService(SqlMiddlewareService delegate, MiddlewareTelemetry telemetry) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry must not be null");
    }

    /**
     * Delegates analyze and records operation telemetry.
     *
     * @param request analyze request payload
     * @return decision result payload
     */
    @Override
    public DecisionResultDto analyze(AnalyzeRequest request) {
        var started = System.nanoTime();
        var result = delegate.analyze(request);
        telemetry.record("analyze", result, System.nanoTime() - started);
        return result;
    }

    /**
     * Delegates enforce and records operation telemetry.
     *
     * @param request enforce request payload
     * @return decision result payload
     */
    @Override
    public DecisionResultDto enforce(EnforceRequest request) {
        var started = System.nanoTime();
        var result = delegate.enforce(request);
        telemetry.record("enforce", result, System.nanoTime() - started);
        return result;
    }

    /**
     * Delegates explain and records operation telemetry for the returned decision.
     *
     * @param request explain request payload
     * @return decision explanation payload
     */
    @Override
    public DecisionExplanationDto explainDecision(ExplainRequest request) {
        var started = System.nanoTime();
        var result = delegate.explainDecision(request);
        telemetry.record("explain", result.decision(), System.nanoTime() - started);
        return result;
    }
}
