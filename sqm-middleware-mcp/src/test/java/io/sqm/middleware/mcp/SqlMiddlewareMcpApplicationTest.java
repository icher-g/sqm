package io.sqm.middleware.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sqm.middleware.api.*;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlMiddlewareMcpApplicationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void server_handles_initialize_and_tools_call_over_framed_stdio() {
        var service = new StubService();
        var router = new SqlMiddlewareMcpToolRouter(new SqlMiddlewareMcpAdapter(service));
        var server = new SqlMiddlewareMcpServer(router, MAPPER);

        var initialize = framedJson("""
            {"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}
            """);
        var call = framedJson("""
            {"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"middleware.analyze","arguments":{"sql":"select 1","context":{"dialect":"postgresql"}}}}
            """);
        var exit = framedJson("""
            {"jsonrpc":"2.0","method":"exit"}
            """);

        var input = new ByteArrayInputStream((initialize + call + exit).getBytes(StandardCharsets.UTF_8));
        var output = new ByteArrayOutputStream();

        assertDoesNotThrow(() -> server.serve(input, output));

        var payload = output.toString(StandardCharsets.UTF_8);
        assertTrue(payload.contains("\"id\":1"));
        assertTrue(payload.contains("\"id\":2"));
        assertTrue(payload.contains("middleware.analyze") || payload.contains("structuredContent"));
    }

    private String framedJson(String json) {
        var bytes = json.getBytes(StandardCharsets.UTF_8);
        return "Content-Length: " + bytes.length + "\r\n\r\n" + json;
    }

    private static final class StubService implements SqlMiddlewareService {

        @Override
        public DecisionResultDto analyze(AnalyzeRequest request) {
            return new DecisionResultDto(DecisionKindDto.ALLOW, ReasonCodeDto.NONE, "ok", null, List.of(), null, null);
        }

        @Override
        public DecisionResultDto enforce(EnforceRequest request) {
            return new DecisionResultDto(DecisionKindDto.REWRITE, ReasonCodeDto.REWRITE_LIMIT, "limited", "select 1 limit ?", List.of(10), null, null);
        }

        @Override
        public DecisionExplanationDto explainDecision(ExplainRequest request) {
            return new DecisionExplanationDto(
                new DecisionResultDto(DecisionKindDto.ALLOW, ReasonCodeDto.NONE, "ok", null, List.of(), null, null),
                "explanation"
            );
        }
    }
}
