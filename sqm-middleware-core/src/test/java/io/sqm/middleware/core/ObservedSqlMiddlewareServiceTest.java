package io.sqm.middleware.core;

import io.sqm.middleware.api.AnalyzeRequest;
import io.sqm.middleware.api.DecisionExplanationDto;
import io.sqm.middleware.api.DecisionKindDto;
import io.sqm.middleware.api.DecisionResultDto;
import io.sqm.middleware.api.EnforceRequest;
import io.sqm.middleware.api.ExecutionContextDto;
import io.sqm.middleware.api.ExplainRequest;
import io.sqm.middleware.api.ReasonCodeDto;
import io.sqm.middleware.api.SqlMiddlewareService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservedSqlMiddlewareServiceTest {

    @Test
    void records_telemetry_for_all_operations() {
        var capture = new CaptureTelemetry();
        var service = new ObservedSqlMiddlewareService(new StubService(), capture);
        var context = new ExecutionContextDto("postgresql", null, null, null, null);

        service.analyze(new AnalyzeRequest("select 1", context));
        assertEquals("analyze", capture.lastOperation);
        assertEquals(DecisionKindDto.ALLOW, capture.lastDecision.kind());
        assertTrue(capture.lastDurationNanos >= 0L);

        service.enforce(new EnforceRequest("delete from users", context));
        assertEquals("enforce", capture.lastOperation);
        assertEquals(DecisionKindDto.DENY, capture.lastDecision.kind());

        service.explainDecision(new ExplainRequest("select 1", context));
        assertEquals("explain", capture.lastOperation);
        assertEquals(DecisionKindDto.REWRITE, capture.lastDecision.kind());
    }

    private static final class CaptureTelemetry implements MiddlewareTelemetry {
        private String lastOperation;
        private DecisionResultDto lastDecision;
        private long lastDurationNanos;

        @Override
        public void record(String operation, DecisionResultDto decision, long durationNanos) {
            this.lastOperation = operation;
            this.lastDecision = decision;
            this.lastDurationNanos = durationNanos;
        }
    }

    private static final class StubService implements SqlMiddlewareService {
        @Override
        public DecisionResultDto analyze(AnalyzeRequest request) {
            return new DecisionResultDto(DecisionKindDto.ALLOW, ReasonCodeDto.NONE, null, null, java.util.List.of(), null, null);
        }

        @Override
        public DecisionResultDto enforce(EnforceRequest request) {
            return new DecisionResultDto(DecisionKindDto.DENY, ReasonCodeDto.DENY_DML, "blocked", null, java.util.List.of(), null, null);
        }

        @Override
        public DecisionExplanationDto explainDecision(ExplainRequest request) {
            var decision = new DecisionResultDto(
                DecisionKindDto.REWRITE,
                ReasonCodeDto.REWRITE_LIMIT,
                "rewritten",
                "select 1 limit 10",
                java.util.List.of(),
                "fp",
                null
            );
            return new DecisionExplanationDto(decision, "explain");
        }
    }
}
