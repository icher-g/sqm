package io.sqm.middleware.core;

import io.sqm.control.DecisionExplanation;
import io.sqm.control.DecisionGuidance;
import io.sqm.control.DecisionResult;
import io.sqm.control.ExecutionContext;
import io.sqm.control.ExecutionMode;
import io.sqm.control.ParameterizationMode;
import io.sqm.control.SqlMiddleware;
import io.sqm.middleware.api.AnalyzeRequest;
import io.sqm.middleware.api.DecisionExplanationDto;
import io.sqm.middleware.api.DecisionGuidanceDto;
import io.sqm.middleware.api.DecisionKindDto;
import io.sqm.middleware.api.DecisionResultDto;
import io.sqm.middleware.api.EnforceRequest;
import io.sqm.middleware.api.ExecutionContextDto;
import io.sqm.middleware.api.ExecutionModeDto;
import io.sqm.middleware.api.ExplainRequest;
import io.sqm.middleware.api.ParameterizationModeDto;
import io.sqm.middleware.api.ReasonCodeDto;
import io.sqm.middleware.api.SqlMiddlewareService;

import java.util.Objects;

/**
 * Default transport-neutral middleware service implementation backed by {@link SqlMiddleware}.
 */
public final class SqlMiddlewareCoreService implements SqlMiddlewareService {

    private final SqlMiddleware middleware;

    /**
     * Creates a service backed by middleware.
     *
     * @param middleware middleware implementation
     */
    public SqlMiddlewareCoreService(SqlMiddleware middleware) {
        this.middleware = Objects.requireNonNull(middleware, "middleware must not be null");
    }

    /**
     * Analyzes SQL through the underlying middleware in analyze mode.
     *
     * @param request analyze request payload
     * @return decision result payload
     */
    @Override
    public DecisionResultDto analyze(AnalyzeRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        var context = toExecutionContext(request.context(), ExecutionMode.ANALYZE);
        var decision = middleware.analyze(request.sql(), context);
        return toDecisionResultDto(decision);
    }

    /**
     * Enforces policies through the underlying middleware in execute-intent mode.
     *
     * @param request enforce request payload
     * @return decision result payload
     */
    @Override
    public DecisionResultDto enforce(EnforceRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        var context = toExecutionContext(request.context(), ExecutionMode.EXECUTE);
        var decision = middleware.enforce(request.sql(), context);
        return toDecisionResultDto(decision);
    }

    /**
     * Produces a decision explanation through the underlying middleware.
     *
     * @param request explain request payload
     * @return decision explanation payload
     */
    @Override
    public DecisionExplanationDto explainDecision(ExplainRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        var context = toExecutionContext(request.context(), ExecutionMode.ANALYZE);
        var explanation = middleware.explainDecision(request.sql(), context);
        return toDecisionExplanationDto(explanation);
    }

    private ExecutionContext toExecutionContext(ExecutionContextDto dto, ExecutionMode defaultMode) {
        Objects.requireNonNull(dto, "context must not be null");
        var mode = parseExecutionMode(dto.mode(), defaultMode);
        var parameterizationMode = parseParameterizationMode(dto.parameterizationMode(), ParameterizationMode.OFF);
        return ExecutionContext.of(dto.dialect(), dto.principal(), dto.tenant(), mode, parameterizationMode);
    }

    private ExecutionMode parseExecutionMode(ExecutionModeDto value, ExecutionMode defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return ExecutionMode.valueOf(value.name());
    }

    private ParameterizationMode parseParameterizationMode(ParameterizationModeDto value, ParameterizationMode defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return ParameterizationMode.valueOf(value.name());
    }

    private DecisionResultDto toDecisionResultDto(DecisionResult result) {
        return new DecisionResultDto(
            DecisionKindDto.valueOf(result.kind().name()),
            ReasonCodeDto.valueOf(result.reasonCode().name()),
            result.message(),
            result.rewrittenSql(),
            result.sqlParams(),
            result.fingerprint(),
            toDecisionGuidanceDto(result.guidance())
        );
    }

    private DecisionExplanationDto toDecisionExplanationDto(DecisionExplanation explanation) {
        return new DecisionExplanationDto(
            toDecisionResultDto(explanation.decision()),
            explanation.explanation()
        );
    }

    private DecisionGuidanceDto toDecisionGuidanceDto(DecisionGuidance guidance) {
        if (guidance == null) {
            return null;
        }
        return new DecisionGuidanceDto(
            guidance.retryable(),
            guidance.remediationHint(),
            guidance.suggestedAction(),
            guidance.retryInstructionHint()
        );
    }
}
