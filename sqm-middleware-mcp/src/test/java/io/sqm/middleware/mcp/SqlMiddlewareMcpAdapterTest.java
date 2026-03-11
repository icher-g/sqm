package io.sqm.middleware.mcp;

import io.sqm.middleware.api.AnalyzeRequest;
import io.sqm.middleware.api.DecisionExplanationDto;
import io.sqm.middleware.api.DecisionGuidanceDto;
import io.sqm.middleware.api.DecisionKindDto;
import io.sqm.middleware.api.DecisionResultDto;
import io.sqm.middleware.api.EnforceRequest;
import io.sqm.middleware.api.ExecutionContextDto;
import io.sqm.middleware.api.ExplainRequest;
import io.sqm.middleware.api.ReasonCodeDto;
import io.sqm.middleware.api.SqlMiddlewareService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SqlMiddlewareMcpAdapterTest {

    @Test
    void delegates_all_operations_to_service() {
        var service = new StubService();
        var adapter = new SqlMiddlewareMcpAdapter(service);
        var ctx = new ExecutionContextDto("postgresql", null, null, null, null);

        var analyze = adapter.analyze(new AnalyzeRequest("select 1", ctx));
        var enforce = adapter.enforce(new EnforceRequest("select 1", ctx));
        var explain = adapter.explain(new ExplainRequest("select 1", ctx));

        assertEquals("analyze", analyze.message());
        assertEquals(DecisionKindDto.DENY, enforce.kind());
        assertEquals("explain", explain.explanation());
    }

    @Test
    void delegates_mysql_dml_requests_without_changing_payload() {
        var service = new CapturingService();
        var adapter = new SqlMiddlewareMcpAdapter(service);
        var context = new ExecutionContextDto("mysql", "api-user", "tenant-mysql", null, null);
        var request = new ExplainRequest("delete from users where id = 1", context);

        var result = adapter.explain(request);

        assertEquals("ok", result.explanation());
        assertEquals("mysql", service.lastExplain.context().dialect());
        assertEquals("delete from users where id = 1", service.lastExplain.sql());
    }

    private static final class StubService implements SqlMiddlewareService {

        @Override
        public DecisionResultDto analyze(AnalyzeRequest request) {
            return new DecisionResultDto(DecisionKindDto.ALLOW, ReasonCodeDto.NONE, "analyze", null, java.util.List.of(), null, null);
        }

        @Override
        public DecisionResultDto enforce(EnforceRequest request) {
            return new DecisionResultDto(
                DecisionKindDto.DENY,
                ReasonCodeDto.DENY_DML,
                "enforce",
                null,
                java.util.List.of(),
                null,
                new DecisionGuidanceDto(false, "remove DML", "remove_dml", null)
            );
        }

        @Override
        public DecisionExplanationDto explainDecision(ExplainRequest request) {
            return new DecisionExplanationDto(
                new DecisionResultDto(DecisionKindDto.ALLOW, ReasonCodeDto.NONE, null, null, java.util.List.of(), null, null),
                "explain"
            );
        }
    }

    private static final class CapturingService implements SqlMiddlewareService {
        private ExplainRequest lastExplain;

        @Override
        public DecisionResultDto analyze(AnalyzeRequest request) {
            return new DecisionResultDto(DecisionKindDto.ALLOW, ReasonCodeDto.NONE, null, null, List.of(), null, null);
        }

        @Override
        public DecisionResultDto enforce(EnforceRequest request) {
            return new DecisionResultDto(DecisionKindDto.ALLOW, ReasonCodeDto.NONE, null, null, List.of(), null, null);
        }

        @Override
        public DecisionExplanationDto explainDecision(ExplainRequest request) {
            lastExplain = request;
            return new DecisionExplanationDto(
                new DecisionResultDto(DecisionKindDto.ALLOW, ReasonCodeDto.NONE, null, null, List.of(), null, null),
                "ok"
            );
        }
    }
}
