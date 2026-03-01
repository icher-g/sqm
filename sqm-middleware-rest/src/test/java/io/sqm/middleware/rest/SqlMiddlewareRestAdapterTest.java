package io.sqm.middleware.rest;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlMiddlewareRestAdapterTest {

    @Test
    void rejects_null_service_in_constructor() {
        assertThrows(NullPointerException.class, () -> new SqlMiddlewareRestAdapter(null));
    }

    @Test
    void delegates_all_operations_to_service() {
        var service = new StubService();
        var adapter = new SqlMiddlewareRestAdapter(service);
        var ctx = new ExecutionContextDto("postgresql", null, null, null, null);

        var analyze = adapter.analyze(new AnalyzeRequest("select 1", ctx));
        var enforce = adapter.enforce(new EnforceRequest("select 1", ctx));
        var explain = adapter.explain(new ExplainRequest("select 1", ctx));

        assertEquals("analyze", analyze.message());
        assertEquals(DecisionKindDto.DENY, enforce.kind());
        assertEquals("explain", explain.explanation());
    }

    @Test
    void rejects_blank_sql_with_invalid_request_exception() {
        var adapter = new SqlMiddlewareRestAdapter(new StubService());
        var ctx = new ExecutionContextDto("postgresql", null, null, null, null);

        var error = assertThrows(
            InvalidRequestException.class,
            () -> adapter.analyze(new AnalyzeRequest(" ", ctx))
        );

        assertEquals("INVALID_REQUEST", error.code());
    }

    @Test
    void rejects_missing_context_with_invalid_request_exception() {
        var adapter = new SqlMiddlewareRestAdapter(new StubService());

        var error = assertThrows(
            InvalidRequestException.class,
            () -> adapter.enforce(new EnforceRequest("select 1", null))
        );

        assertEquals("INVALID_REQUEST", error.code());
    }

    @Test
    void rejects_blank_context_dialect_with_invalid_request_exception() {
        var adapter = new SqlMiddlewareRestAdapter(new StubService());
        var ctx = new ExecutionContextDto(" ", null, null, null, null);

        var error = assertThrows(
            InvalidRequestException.class,
            () -> adapter.explain(new ExplainRequest("select 1", ctx))
        );

        assertEquals("INVALID_REQUEST", error.code());
    }

    @Test
    void rejects_null_requests_for_all_operations() {
        var adapter = new SqlMiddlewareRestAdapter(new StubService());

        var analyze = assertThrows(InvalidRequestException.class, () -> adapter.analyze(null));
        var enforce = assertThrows(InvalidRequestException.class, () -> adapter.enforce(null));
        var explain = assertThrows(InvalidRequestException.class, () -> adapter.explain(null));

        assertEquals("INVALID_REQUEST", analyze.code());
        assertEquals("INVALID_REQUEST", enforce.code());
        assertEquals("INVALID_REQUEST", explain.code());
        assertNotNull(analyze.getMessage());
        assertNotNull(enforce.getMessage());
        assertNotNull(explain.getMessage());
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
}
