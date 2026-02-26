package io.sqm.middleware.mcp;

import io.sqm.middleware.api.AnalyzeRequest;
import io.sqm.middleware.api.DecisionExplanationDto;
import io.sqm.middleware.api.DecisionKindDto;
import io.sqm.middleware.api.DecisionResultDto;
import io.sqm.middleware.api.EnforceRequest;
import io.sqm.middleware.api.ExecutionContextDto;
import io.sqm.middleware.api.ParameterizationModeDto;
import io.sqm.middleware.api.ReasonCodeDto;
import io.sqm.middleware.api.SqlMiddlewareService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlMiddlewareMcpToolRouterTest {

    @Test
    void routes_known_tool_names_to_adapter_methods() {
        var service = new StubService();
        var adapter = new SqlMiddlewareMcpAdapter(service);
        var router = new SqlMiddlewareMcpToolRouter(adapter);
        var context = new ExecutionContextDto("postgresql", null, null, null, ParameterizationModeDto.OFF);

        var analyze = (DecisionResultDto) router.invoke(SqlMiddlewareMcpToolRouter.ANALYZE_TOOL, new AnalyzeRequest("select 1", context));
        var enforce = (DecisionResultDto) router.invoke(SqlMiddlewareMcpToolRouter.ENFORCE_TOOL, new EnforceRequest("select 1", context));

        assertEquals(DecisionKindDto.ALLOW, analyze.kind());
        assertEquals(DecisionKindDto.REWRITE, enforce.kind());
    }

    @Test
    void throws_for_unknown_tool_or_wrong_request_type() {
        var service = new StubService();
        var adapter = new SqlMiddlewareMcpAdapter(service);
        var router = new SqlMiddlewareMcpToolRouter(adapter);

        assertThrows(IllegalArgumentException.class, () -> router.invoke("middleware.unknown", new Object()));
        assertThrows(IllegalArgumentException.class, () -> router.invoke(SqlMiddlewareMcpToolRouter.ANALYZE_TOOL, new Object()));
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
        public DecisionExplanationDto explainDecision(io.sqm.middleware.api.ExplainRequest request) {
            return new DecisionExplanationDto(
                new DecisionResultDto(DecisionKindDto.ALLOW, ReasonCodeDto.NONE, null, null, List.of(), null, null),
                "ok"
            );
        }
    }
}
