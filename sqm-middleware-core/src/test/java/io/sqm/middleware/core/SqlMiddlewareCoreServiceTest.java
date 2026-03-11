package io.sqm.middleware.core;

import io.sqm.control.decision.DecisionExplanation;
import io.sqm.control.decision.DecisionGuidance;
import io.sqm.control.decision.DecisionResult;
import io.sqm.control.decision.ReasonCode;
import io.sqm.control.execution.ExecutionContext;
import io.sqm.control.execution.ExecutionMode;
import io.sqm.control.execution.ParameterizationMode;
import io.sqm.control.service.SqlDecisionService;
import io.sqm.middleware.api.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SqlMiddlewareCoreServiceTest {

    @Test
    void analyze_maps_context_and_returns_allow_decision() {
        var decisionService = new CapturingMiddleware();
        var service = new SqlMiddlewareCoreService(decisionService);

        var request = new AnalyzeRequest(
            "select 1",
            new ExecutionContextDto("postgresql", "alice", "tenant-a", ExecutionModeDto.ANALYZE, ParameterizationModeDto.BIND)
        );

        var result = service.analyze(request);

        assertEquals(DecisionKindDto.ALLOW, result.kind());
        assertEquals(ReasonCodeDto.NONE, result.reasonCode());
        assertEquals(ExecutionMode.ANALYZE, decisionService.lastContext.mode());
        assertEquals(ParameterizationMode.BIND, decisionService.lastContext.parameterizationMode());
        assertEquals("alice", decisionService.lastContext.principal());
        assertEquals("tenant-a", decisionService.lastContext.tenant());
    }

    @Test
    void enforce_uses_execute_mode_when_request_mode_is_blank() {
        var decisionService = new CapturingMiddleware();
        var service = new SqlMiddlewareCoreService(decisionService);

        var request = new EnforceRequest(
            "select 1",
            new ExecutionContextDto("postgresql", null, null, null, null)
        );

        service.enforce(request);

        assertEquals(ExecutionMode.EXECUTE, decisionService.lastContext.mode());
        assertEquals(ParameterizationMode.OFF, decisionService.lastContext.parameterizationMode());
    }

    @Test
    void explain_preserves_decision_and_explanation_shape() {
        var decisionService = new CapturingMiddleware();
        var service = new SqlMiddlewareCoreService(decisionService);

        var request = new ExplainRequest(
            "select 1",
            new ExecutionContextDto("postgresql", null, null, null, null)
        );

        var result = service.explainDecision(request);

        assertEquals("basic explanation", result.explanation());
    }

    @Test
    void analyze_defaults_to_analyze_when_mode_is_missing() {
        var decisionService = new CapturingMiddleware();
        var service = new SqlMiddlewareCoreService(decisionService);
        var request = new AnalyzeRequest(
            "select 1",
            new ExecutionContextDto("postgresql", null, null, null, null)
        );

        service.analyze(request);

        assertEquals(ExecutionMode.ANALYZE, decisionService.lastContext.mode());
    }

    @Test
    void enforce_preserves_guidance_and_kind() {
        var decisionService = new CapturingMiddleware();
        var service = new SqlMiddlewareCoreService(decisionService);

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

    @Test
    void analyze_and_enforce_preserve_mysql_dml_context() {
        var decisionService = new CapturingMiddleware();
        var service = new SqlMiddlewareCoreService(decisionService);
        var analyzeRequest = new AnalyzeRequest(
            "insert ignore into users (id, name) values (1, 'alice')",
            new ExecutionContextDto("mysql", "api-user", "tenant-mysql", ExecutionModeDto.ANALYZE, ParameterizationModeDto.OFF)
        );
        var enforceRequest = new EnforceRequest(
            "update users set name = 'alice' where id = 1",
            new ExecutionContextDto("mysql", "api-user", "tenant-mysql", ExecutionModeDto.EXECUTE, ParameterizationModeDto.BIND)
        );

        service.analyze(analyzeRequest);
        assertEquals("mysql", decisionService.lastContext.dialect());
        assertEquals(ExecutionMode.ANALYZE, decisionService.lastContext.mode());
        assertEquals("tenant-mysql", decisionService.lastContext.tenant());

        service.enforce(enforceRequest);
        assertEquals("mysql", decisionService.lastContext.dialect());
        assertEquals(ExecutionMode.EXECUTE, decisionService.lastContext.mode());
        assertEquals(ParameterizationMode.BIND, decisionService.lastContext.parameterizationMode());
    }

    private static final class CapturingMiddleware implements SqlDecisionService {

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

