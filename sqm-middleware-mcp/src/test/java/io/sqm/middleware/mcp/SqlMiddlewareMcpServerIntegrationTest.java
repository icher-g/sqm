package io.sqm.middleware.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sqm.middleware.api.AnalyzeRequest;
import io.sqm.middleware.api.DecisionExplanationDto;
import io.sqm.middleware.api.DecisionKindDto;
import io.sqm.middleware.api.DecisionResultDto;
import io.sqm.middleware.api.EnforceRequest;
import io.sqm.middleware.api.ExplainRequest;
import io.sqm.middleware.api.ReasonCodeDto;
import io.sqm.middleware.api.SqlMiddlewareService;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SqlMiddlewareMcpServerIntegrationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void server_returns_initialize_and_tools_list_results_over_framed_protocol() throws Exception {
        var server = new SqlMiddlewareMcpServer(routerFor(new StubService()), MAPPER);
        var input = new ByteArrayInputStream((
            framedJson("""
                {"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}
                """)
                + framedJson("""
                {"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}
                """)
                + framedJson("""
                {"jsonrpc":"2.0","method":"exit"}
                """)
        ).getBytes(StandardCharsets.UTF_8));
        var output = new ByteArrayOutputStream();

        server.serve(input, output);

        var responses = parseFramedResponses(output.toByteArray());
        assertEquals(2, responses.size());

        var initialize = responses.getFirst();
        assertEquals("2.0", initialize.path("jsonrpc").asText());
        assertEquals(1, initialize.path("id").asInt());
        assertEquals("sqm-middleware-mcp", initialize.path("result").path("serverInfo").path("name").asText());

        var toolsList = responses.get(1);
        assertEquals("2.0", toolsList.path("jsonrpc").asText());
        assertEquals(2, toolsList.path("id").asInt());
        var tools = toolsList.path("result").path("tools");
        assertTrue(tools.isArray());
        assertEquals(3, tools.size());
        assertTrue(containsTool(tools, "middleware.analyze"));
        assertTrue(containsTool(tools, "middleware.enforce"));
        assertTrue(containsTool(tools, "middleware.explain"));
    }

    @Test
    void server_routes_tools_call_for_all_operations_and_returns_structured_content() throws Exception {
        var server = new SqlMiddlewareMcpServer(routerFor(new StubService()), MAPPER);
        var input = new ByteArrayInputStream((
            framedJson("""
                {"jsonrpc":"2.0","id":11,"method":"tools/call","params":{"name":"middleware.analyze","arguments":{"sql":"select 1","context":{"dialect":"postgresql"}}}}
                """)
                + framedJson("""
                {"jsonrpc":"2.0","id":12,"method":"tools/call","params":{"name":"middleware.enforce","arguments":{"sql":"drop table users","context":{"dialect":"postgresql"}}}}
                """)
                + framedJson("""
                {"jsonrpc":"2.0","id":13,"method":"tools/call","params":{"name":"middleware.explain","arguments":{"sql":"select 1","context":{"dialect":"postgresql"}}}}
                """)
                + framedJson("""
                {"jsonrpc":"2.0","method":"exit"}
                """)
        ).getBytes(StandardCharsets.UTF_8));
        var output = new ByteArrayOutputStream();

        server.serve(input, output);

        var responses = parseFramedResponses(output.toByteArray());
        assertEquals(3, responses.size());

        var analyze = responses.getFirst();
        assertEquals(11, analyze.path("id").asInt());
        assertEquals("ALLOW", analyze.path("result").path("structuredContent").path("kind").asText());

        var enforce = responses.get(1);
        assertEquals(12, enforce.path("id").asInt());
        assertEquals("DENY", enforce.path("result").path("structuredContent").path("kind").asText());
        assertEquals("DENY_DDL", enforce.path("result").path("structuredContent").path("reasonCode").asText());

        var explain = responses.get(2);
        assertEquals(13, explain.path("id").asInt());
        assertEquals("ALLOW", explain.path("result").path("structuredContent").path("decision").path("kind").asText());
        assertNotNull(explain.path("result").path("content"));
        assertTrue(explain.path("result").path("content").isArray());
        assertFalse(explain.path("result").path("content").isEmpty());
    }

    @Test
    void server_returns_json_rpc_error_for_unknown_method() throws Exception {
        var server = new SqlMiddlewareMcpServer(routerFor(new StubService()), MAPPER);
        var input = new ByteArrayInputStream((
            framedJson("""
                {"jsonrpc":"2.0","id":99,"method":"unknown.method","params":{}}
                """)
                + framedJson("""
                {"jsonrpc":"2.0","method":"exit"}
                """)
        ).getBytes(StandardCharsets.UTF_8));
        var output = new ByteArrayOutputStream();

        server.serve(input, output);

        var responses = parseFramedResponses(output.toByteArray());
        assertEquals(1, responses.size());

        var error = responses.getFirst();
        assertEquals(99, error.path("id").asInt());
        assertEquals(-32601, error.path("error").path("code").asInt());
        assertTrue(error.path("error").path("message").asText().contains("Method not found"));
    }

    @Test
    void server_throws_when_frame_missing_content_length_header() {
        var server = new SqlMiddlewareMcpServer(routerFor(new StubService()), MAPPER);
        var malformed = "Content-Type: application/json\r\n\r\n{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}";
        var input = new ByteArrayInputStream(malformed.getBytes(StandardCharsets.UTF_8));
        var output = new ByteArrayOutputStream();

        var error = assertThrows(IOException.class, () -> server.serve(input, output));
        assertTrue(error.getMessage().contains("Missing Content-Length header"));
    }

    private SqlMiddlewareMcpToolRouter routerFor(SqlMiddlewareService service) {
        return new SqlMiddlewareMcpToolRouter(new SqlMiddlewareMcpAdapter(service));
    }

    private boolean containsTool(JsonNode tools, String name) {
        for (JsonNode tool : tools) {
            if (name.equals(tool.path("name").asText())) {
                return true;
            }
        }
        return false;
    }

    private String framedJson(String json) {
        var bytes = json.getBytes(StandardCharsets.UTF_8);
        return "Content-Length: " + bytes.length + "\r\n\r\n" + json;
    }

    private List<JsonNode> parseFramedResponses(byte[] bytes) throws Exception {
        var responses = new ArrayList<JsonNode>();
        var index = 0;

        while (index < bytes.length) {
            var headerEnd = findHeaderEnd(bytes, index);
            if (headerEnd < 0) {
                break;
            }

            var header = new String(bytes, index, headerEnd - index, StandardCharsets.UTF_8);
            var contentLength = parseContentLength(header);
            var bodyStart = headerEnd + 4;
            var body = new String(bytes, bodyStart, contentLength, StandardCharsets.UTF_8);
            responses.add(MAPPER.readTree(body));

            index = bodyStart + contentLength;
        }

        return responses;
    }

    private int findHeaderEnd(byte[] bytes, int start) {
        for (int i = start; i + 3 < bytes.length; i++) {
            if (bytes[i] == '\r' && bytes[i + 1] == '\n' && bytes[i + 2] == '\r' && bytes[i + 3] == '\n') {
                return i;
            }
        }
        return -1;
    }

    private int parseContentLength(String header) {
        var lines = header.split("\\r\\n");
        for (String line : lines) {
            if (line.toLowerCase().startsWith("content-length:")) {
                return Integer.parseInt(line.substring("Content-Length:".length()).trim());
            }
        }
        throw new IllegalStateException("Missing Content-Length in header: " + header);
    }

    private static final class StubService implements SqlMiddlewareService {

        @Override
        public DecisionResultDto analyze(AnalyzeRequest request) {
            return new DecisionResultDto(DecisionKindDto.ALLOW, ReasonCodeDto.NONE, "ok", null, List.of(), null, null);
        }

        @Override
        public DecisionResultDto enforce(EnforceRequest request) {
            return new DecisionResultDto(DecisionKindDto.DENY, ReasonCodeDto.DENY_DDL, "ddl denied", null, List.of(), null, null);
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
