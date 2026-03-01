package io.sqm.middleware.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sqm.middleware.api.*;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
        assertFalse(initialize.path("result").path("serverInfo").path("version").asText().isBlank());

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
                {"jsonrpc":"2.0","id":10,"method":"initialize","params":{}}
                """)
                +
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
        assertEquals(4, responses.size());

        var analyze = responses.get(1);
        assertEquals(11, analyze.path("id").asInt());
        assertEquals("ALLOW", analyze.path("result").path("structuredContent").path("kind").asText());

        var enforce = responses.get(2);
        assertEquals(12, enforce.path("id").asInt());
        assertEquals("DENY", enforce.path("result").path("structuredContent").path("kind").asText());
        assertEquals("DENY_DDL", enforce.path("result").path("structuredContent").path("reasonCode").asText());

        var explain = responses.get(3);
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
    void server_throws_when_frame_missing_content_length_header() throws Exception {
        var server = new SqlMiddlewareMcpServer(routerFor(new StubService()), MAPPER);
        var malformed = "Content-Type: application/json\r\n\r\n{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}";
        var input = new ByteArrayInputStream(malformed.getBytes(StandardCharsets.UTF_8));
        var output = new ByteArrayOutputStream();

        assertDoesNotThrow(() -> server.serve(input, output));
        var responses = parseFramedResponses(output.toByteArray());
        assertEquals(1, responses.size());
        var error = responses.getFirst();
        assertEquals(-32600, error.path("error").path("code").asInt());
        assertEquals("INVALID_FRAME", error.path("error").path("data").path("category").asText());
    }

    @Test
    void server_returns_parse_error_for_invalid_json_payload() throws Exception {
        var server = new SqlMiddlewareMcpServer(routerFor(new StubService()), MAPPER);
        var input = new ByteArrayInputStream((
            framedJson("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":")
                + framedJson("""
                {"jsonrpc":"2.0","method":"exit"}
                """)
        ).getBytes(StandardCharsets.UTF_8));
        var output = new ByteArrayOutputStream();

        server.serve(input, output);

        var responses = parseFramedResponses(output.toByteArray());
        assertEquals(1, responses.size());
        var error = responses.getFirst();
        assertEquals(-32700, error.path("error").path("code").asInt());
        assertTrue(error.path("error").path("message").asText().contains("Parse error"));
    }

    @Test
    void server_returns_invalid_request_when_method_is_missing() throws Exception {
        var server = new SqlMiddlewareMcpServer(routerFor(new StubService()), MAPPER);
        var input = new ByteArrayInputStream((
            framedJson("""
                {"jsonrpc":"2.0","id":7,"params":{}}
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
        assertEquals(7, error.path("id").asInt());
        assertEquals(-32600, error.path("error").path("code").asInt());
    }

    @Test
    void server_returns_internal_error_when_tools_call_params_are_missing() throws Exception {
        var server = new SqlMiddlewareMcpServer(routerFor(new StubService()), MAPPER);
        var input = new ByteArrayInputStream((
            framedJson("""
                {"jsonrpc":"2.0","id":7,"method":"initialize","params":{}}
                """)
                +
                framedJson("""
                    {"jsonrpc":"2.0","id":8,"method":"tools/call"}
                    """)
                + framedJson("""
                {"jsonrpc":"2.0","method":"exit"}
                """)
        ).getBytes(StandardCharsets.UTF_8));
        var output = new ByteArrayOutputStream();

        server.serve(input, output);

        var responses = parseFramedResponses(output.toByteArray());
        assertEquals(2, responses.size());
        var error = responses.get(1);
        assertEquals(8, error.path("id").asInt());
        assertEquals(-32602, error.path("error").path("code").asInt());
        assertTrue(error.path("error").path("message").asText().contains("tools/call"));
        assertEquals("INVALID_PARAMS", error.path("error").path("data").path("category").asText());
    }

    @Test
    void server_maps_tools_call_unknown_tool_to_invalid_params() throws Exception {
        var server = new SqlMiddlewareMcpServer(routerFor(new StubService()), MAPPER);
        var input = new ByteArrayInputStream((
            framedJson("""
                {"jsonrpc":"2.0","id":17,"method":"initialize","params":{}}
                """)
                +
                framedJson("""
                    {"jsonrpc":"2.0","id":18,"method":"tools/call","params":{"name":"middleware.unknown","arguments":{"sql":"select 1","context":{"dialect":"postgresql"}}}}
                    """)
                + framedJson("""
                {"jsonrpc":"2.0","method":"exit"}
                """)
        ).getBytes(StandardCharsets.UTF_8));
        var output = new ByteArrayOutputStream();

        server.serve(input, output);

        var responses = parseFramedResponses(output.toByteArray());
        assertEquals(2, responses.size());
        var error = responses.get(1);
        assertEquals(18, error.path("id").asInt());
        assertEquals(-32602, error.path("error").path("code").asInt());
        assertEquals("INVALID_PARAMS", error.path("error").path("data").path("category").asText());
    }

    @Test
    void server_maps_unexpected_tool_execution_failure_to_internal_error() throws Exception {
        var server = new SqlMiddlewareMcpServer(routerFor(new ThrowingService()), MAPPER);
        var input = new ByteArrayInputStream((
            framedJson("""
                {"jsonrpc":"2.0","id":30,"method":"initialize","params":{}}
                """)
                +
                framedJson("""
                    {"jsonrpc":"2.0","id":31,"method":"tools/call","params":{"name":"middleware.analyze","arguments":{"sql":"select 1","context":{"dialect":"postgresql"}}}}
                    """)
                + framedJson("""
                {"jsonrpc":"2.0","method":"exit"}
                """)
        ).getBytes(StandardCharsets.UTF_8));
        var output = new ByteArrayOutputStream();

        server.serve(input, output);

        var responses = parseFramedResponses(output.toByteArray());
        assertEquals(2, responses.size());
        var error = responses.get(1);
        assertEquals(31, error.path("id").asInt());
        assertEquals(-32603, error.path("error").path("code").asInt());
        assertEquals("INTERNAL_ERROR", error.path("error").path("data").path("category").asText());
        assertTrue(error.path("error").path("data").path("detail").asText().contains("boom"));
    }

    @Test
    void server_handles_shutdown_request_with_null_result() throws Exception {
        var server = new SqlMiddlewareMcpServer(routerFor(new StubService()), MAPPER);
        var input = new ByteArrayInputStream((
            framedJson("""
                {"jsonrpc":"2.0","id":21,"method":"shutdown","params":{}}
                """)
                + framedJson("""
                {"jsonrpc":"2.0","method":"exit"}
                """)
        ).getBytes(StandardCharsets.UTF_8));
        var output = new ByteArrayOutputStream();

        server.serve(input, output);

        var responses = parseFramedResponses(output.toByteArray());
        assertEquals(1, responses.size());
        var shutdown = responses.getFirst();
        assertEquals(21, shutdown.path("id").asInt());
        assertTrue(shutdown.has("result"));
        assertTrue(shutdown.path("result").isNull());
    }

    @Test
    void server_ignores_unknown_notification_without_id() throws Exception {
        var server = new SqlMiddlewareMcpServer(routerFor(new StubService()), MAPPER);
        var input = new ByteArrayInputStream((
            framedJson("""
                {"jsonrpc":"2.0","method":"unknown.notification","params":{}}
                """)
                + framedJson("""
                {"jsonrpc":"2.0","method":"exit"}
                """)
        ).getBytes(StandardCharsets.UTF_8));
        var output = new ByteArrayOutputStream();

        server.serve(input, output);

        var responses = parseFramedResponses(output.toByteArray());
        assertTrue(responses.isEmpty());
    }

    @Test
    void server_throws_when_frame_body_is_truncated() throws Exception {
        var server = new SqlMiddlewareMcpServer(routerFor(new StubService()), MAPPER);
        var malformed = "Content-Length: 200\r\n\r\n{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}";
        var input = new ByteArrayInputStream(malformed.getBytes(StandardCharsets.UTF_8));
        var output = new ByteArrayOutputStream();

        assertDoesNotThrow(() -> server.serve(input, output));
        var responses = parseFramedResponses(output.toByteArray());
        assertEquals(1, responses.size());
        var error = responses.getFirst();
        assertEquals(-32600, error.path("error").path("code").asInt());
        assertEquals("INVALID_FRAME", error.path("error").path("data").path("category").asText());
    }

    @Test
    void server_denies_tools_call_before_initialize_when_required() throws Exception {
        var server = new SqlMiddlewareMcpServer(routerFor(new StubService()), MAPPER);
        var input = new ByteArrayInputStream((
            framedJson("""
                {"jsonrpc":"2.0","id":41,"method":"tools/call","params":{"name":"middleware.analyze","arguments":{"sql":"select 1","context":{"dialect":"postgresql"}}}}
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
        assertEquals(41, error.path("id").asInt());
        assertEquals(-32002, error.path("error").path("code").asInt());
    }

    @Test
    void server_rejects_tools_after_shutdown() throws Exception {
        var server = new SqlMiddlewareMcpServer(routerFor(new StubService()), MAPPER);
        var input = new ByteArrayInputStream((
            framedJson("""
                {"jsonrpc":"2.0","id":51,"method":"initialize","params":{}}
                """)
                + framedJson("""
                {"jsonrpc":"2.0","id":52,"method":"shutdown","params":{}}
                """)
                + framedJson("""
                {"jsonrpc":"2.0","id":53,"method":"tools/list","params":{}}
                """)
                + framedJson("""
                {"jsonrpc":"2.0","method":"exit"}
                """)
        ).getBytes(StandardCharsets.UTF_8));
        var output = new ByteArrayOutputStream();

        server.serve(input, output);

        var responses = parseFramedResponses(output.toByteArray());
        assertEquals(3, responses.size());
        var error = responses.get(2);
        assertEquals(53, error.path("id").asInt());
        assertEquals(-32600, error.path("error").path("code").asInt());
        assertTrue(error.path("error").path("message").asText().contains("shutting down"));
    }

    @Test
    void server_rejects_invalid_jsonrpc_version() throws Exception {
        var server = new SqlMiddlewareMcpServer(routerFor(new StubService()), MAPPER);
        var input = new ByteArrayInputStream((
            framedJson("""
                {"jsonrpc":"1.0","id":61,"method":"initialize","params":{}}
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
        assertEquals(61, error.path("id").asInt());
        assertEquals(-32600, error.path("error").path("code").asInt());
    }

    @Test
    void server_rejects_frame_exceeding_max_content_length() throws Exception {
        var options = new SqlMiddlewareMcpServerOptions(32, 8 * 1024, 16 * 1024, true);
        var server = new SqlMiddlewareMcpServer(routerFor(new StubService()), MAPPER, options);
        var input = new ByteArrayInputStream((
            framedJson("""
                {"jsonrpc":"2.0","id":71,"method":"initialize","params":{"a":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}}
                """)
                + framedJson("""
                {"jsonrpc":"2.0","method":"exit"}
                """)
        ).getBytes(StandardCharsets.UTF_8));
        var output = new ByteArrayOutputStream();

        server.serve(input, output);

        var responses = parseFramedResponses(output.toByteArray());
        assertFalse(responses.isEmpty());
        var error = responses.getFirst();
        assertEquals(-32600, error.path("error").path("code").asInt());
        assertEquals("INVALID_FRAME", error.path("error").path("data").path("category").asText());
    }

    @Test
    void server_allows_tools_without_initialize_when_configured() throws Exception {
        var options = new SqlMiddlewareMcpServerOptions(1024 * 1024, 8 * 1024, 16 * 1024, false);
        var server = new SqlMiddlewareMcpServer(routerFor(new StubService()), MAPPER, options);
        var input = new ByteArrayInputStream((
            framedJson("""
                {"jsonrpc":"2.0","id":81,"method":"tools/list","params":{}}
                """)
                + framedJson("""
                {"jsonrpc":"2.0","method":"exit"}
                """)
        ).getBytes(StandardCharsets.UTF_8));
        var output = new ByteArrayOutputStream();

        server.serve(input, output);
        var responses = parseFramedResponses(output.toByteArray());
        assertEquals(1, responses.size());
        assertTrue(responses.getFirst().has("result"));
        assertFalse(responses.getFirst().has("error"));
    }

    @Test
    void server_rejects_invalid_id_shape() throws Exception {
        var server = new SqlMiddlewareMcpServer(routerFor(new StubService()), MAPPER);
        var input = new ByteArrayInputStream((
            framedJson("""
                {"jsonrpc":"2.0","id":{},"method":"initialize","params":{}}
                """)
                + framedJson("""
                {"jsonrpc":"2.0","method":"exit"}
                """)
        ).getBytes(StandardCharsets.UTF_8));
        var output = new ByteArrayOutputStream();

        server.serve(input, output);
        var responses = parseFramedResponses(output.toByteArray());
        assertEquals(1, responses.size());
        assertEquals(-32600, responses.getFirst().path("error").path("code").asInt());
    }

    @Test
    void server_rejects_when_header_line_exceeds_limit() throws Exception {
        var options = new SqlMiddlewareMcpServerOptions(1024, 12, 1024, true);
        var server = new SqlMiddlewareMcpServer(routerFor(new StubService()), MAPPER, options);
        var body = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}";
        var framed = "Content-Length: " + body.length() + "\r\nX-HEADER-TOO-LONG: abcdefghijklmnop\r\n\r\n" + body;
        var input = new ByteArrayInputStream(framed.getBytes(StandardCharsets.UTF_8));
        var output = new ByteArrayOutputStream();

        server.serve(input, output);
        var responses = parseFramedResponses(output.toByteArray());
        assertFalse(responses.isEmpty());
        var hasInvalidFrame = responses.stream()
            .anyMatch(node -> "INVALID_FRAME".equals(node.path("error").path("data").path("category").asText()));
        assertTrue(hasInvalidFrame);
    }

    @Test
    void server_rejects_when_total_header_bytes_exceed_limit() throws Exception {
        var options = new SqlMiddlewareMcpServerOptions(1024, 200, 24, true);
        var server = new SqlMiddlewareMcpServer(routerFor(new StubService()), MAPPER, options);
        var body = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}";
        var framed = "X-A: 1\r\nX-B: 2\r\nContent-Length: " + body.length() + "\r\n\r\n" + body;
        var input = new ByteArrayInputStream(framed.getBytes(StandardCharsets.UTF_8));
        var output = new ByteArrayOutputStream();

        server.serve(input, output);
        var responses = parseFramedResponses(output.toByteArray());
        assertFalse(responses.isEmpty());
        var hasInvalidFrame = responses.stream()
            .anyMatch(node -> "INVALID_FRAME".equals(node.path("error").path("data").path("category").asText()));
        assertTrue(hasInvalidFrame);
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

    private static final class ThrowingService implements SqlMiddlewareService {

        @Override
        public DecisionResultDto analyze(AnalyzeRequest request) {
            throw new IllegalStateException("boom");
        }

        @Override
        public DecisionResultDto enforce(EnforceRequest request) {
            throw new IllegalStateException("boom");
        }

        @Override
        public DecisionExplanationDto explainDecision(ExplainRequest request) {
            throw new IllegalStateException("boom");
        }
    }
}
