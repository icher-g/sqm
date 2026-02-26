package io.sqm.middleware.core;

import io.sqm.control.*;
import io.sqm.middleware.api.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SqlMiddlewareCoreServiceTest {

    @Test
    void analyze_maps_context_and_returns_allow_decision() {
        var middleware = new CapturingMiddleware();
        var service = new SqlMiddlewareCoreService(middleware);

        var request = new AnalyzeRequest(
            "select 1",
            new ExecutionContextDto("postgresql", "alice", "tenant-a", ExecutionModeDto.ANALYZE, ParameterizationModeDto.BIND)
        );

        var result = service.analyze(request);

        assertEquals(DecisionKindDto.ALLOW, result.kind());
        assertEquals(ReasonCodeDto.NONE, result.reasonCode());
        assertEquals(ExecutionMode.ANALYZE, middleware.lastContext.mode());
        assertEquals(ParameterizationMode.BIND, middleware.lastContext.parameterizationMode());
        assertEquals("alice", middleware.lastContext.principal());
        assertEquals("tenant-a", middleware.lastContext.tenant());
    }

    @Test
    void enforce_uses_execute_mode_when_request_mode_is_blank() {
        var middleware = new CapturingMiddleware();
        var service = new SqlMiddlewareCoreService(middleware);

        var request = new EnforceRequest(
            "select 1",
            new ExecutionContextDto("postgresql", null, null, null, null)
        );

        service.enforce(request);

        assertEquals(ExecutionMode.EXECUTE, middleware.lastContext.mode());
        assertEquals(ParameterizationMode.OFF, middleware.lastContext.parameterizationMode());
    }

    @Test
    void explain_preserves_decision_and_explanation_shape() {
        var middleware = new CapturingMiddleware();
        var service = new SqlMiddlewareCoreService(middleware);

        var request = new ExplainRequest(
            "select 1",
            new ExecutionContextDto("postgresql", null, null, null, null)
        );

        var result = service.explainDecision(request);

        assertEquals("basic explanation", result.explanation());
    }

    @Test
    void analyze_defaults_to_analyze_when_mode_is_missing() {
        var middleware = new CapturingMiddleware();
        var service = new SqlMiddlewareCoreService(middleware);
        var request = new AnalyzeRequest(
            "select 1",
            new ExecutionContextDto("postgresql", null, null, null, null)
        );

        service.analyze(request);

        assertEquals(ExecutionMode.ANALYZE, middleware.lastContext.mode());
    }

    @Test
    void enforce_preserves_guidance_and_kind() {
        var middleware = new CapturingMiddleware();
        var service = new SqlMiddlewareCoreService(middleware);

        var request = new EnforceRequest(
            "delete from users",
            new ExecutionContextDto("postgresql", null, null, null, null)
        );

        var result = service.enforce(request);

        assertEquals(DecisionKindDto.DENY, result.kind());
        assertEquals(ReasonCodeDto.DENY_DML, result.reasonCode());
        assertNotNull(result.guidance());
        assertEquals("remove_dml", result.guidance().suggestedAction());
    }

    private static final class CapturingMiddleware implements SqlMiddleware {

        private ExecutionContext lastContext;

        @Override
        public DecisionResult analyze(String sql, ExecutionContext context) {
            lastContext = context;
            return DecisionResult.allow();
        }

        @Override
        public DecisionResult enforce(String sql, ExecutionContext context) {
            lastContext = context;
            return DecisionResult.deny(
                ReasonCode.DENY_DML,
                "blocked",
                DecisionGuidance.terminal("remove DML", "remove_dml")
            );
        }

        @Override
        public DecisionExplanation explainDecision(String sql, ExecutionContext context) {
            lastContext = context;
            return new DecisionExplanation(DecisionResult.allow(), "basic explanation");
        }
    }
}
