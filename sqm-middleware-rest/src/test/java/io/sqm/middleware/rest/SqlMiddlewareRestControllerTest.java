package io.sqm.middleware.rest;

import io.sqm.middleware.api.AnalyzeRequest;
import io.sqm.middleware.api.DecisionExplanationDto;
import io.sqm.middleware.api.DecisionKindDto;
import io.sqm.middleware.api.DecisionResultDto;
import io.sqm.middleware.api.EnforceRequest;
import io.sqm.middleware.api.ExecutionContextDto;
import io.sqm.middleware.api.ExplainRequest;
import io.sqm.middleware.api.ParameterizationModeDto;
import io.sqm.middleware.api.ReasonCodeDto;
import io.sqm.middleware.api.SqlMiddlewareService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SqlMiddlewareRestControllerTest {

    @Test
    void delegates_http_operations_to_adapter() {
        var service = new StubService();
        var adapter = new SqlMiddlewareRestAdapter(service);
        var controller = new SqlMiddlewareRestController(adapter);

        var context = new ExecutionContextDto("postgresql", "alice", null, null, ParameterizationModeDto.OFF);
        var analyze = controller.analyze(new AnalyzeRequest("select 1", context));
        var enforce = controller.enforce(new EnforceRequest("select 1", context));
        var explain = controller.explain(new ExplainRequest("select 1", context));

        assertEquals(DecisionKindDto.ALLOW, analyze.kind());
        assertEquals(DecisionKindDto.REWRITE, enforce.kind());
        assertEquals("explain", explain.explanation());
    }

    private static final class StubService implements SqlMiddlewareService {

        @Override
        public DecisionResultDto analyze(AnalyzeRequest request) {
            return new DecisionResultDto(DecisionKindDto.ALLOW, ReasonCodeDto.NONE, null, null, List.of(), null, null);
        }

        @Override
        public DecisionResultDto enforce(EnforceRequest request) {
            return new DecisionResultDto(DecisionKindDto.REWRITE, ReasonCodeDto.REWRITE_LIMIT, null, "select 1 limit ?", List.of(10), null, null);
        }

        @Override
        public DecisionExplanationDto explainDecision(ExplainRequest request) {
            return new DecisionExplanationDto(
                new DecisionResultDto(DecisionKindDto.ALLOW, ReasonCodeDto.NONE, null, null, List.of(), null, null),
                "explain"
            );
        }
    }
}
