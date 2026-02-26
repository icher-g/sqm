package io.sqm.middleware.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sqm.middleware.api.AnalyzeRequest;
import io.sqm.middleware.api.EnforceRequest;
import io.sqm.middleware.api.ExplainRequest;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Long-running MCP stdio server using JSON-RPC over Content-Length framed messages.
 */
public final class SqlMiddlewareMcpServer {

    private static final String JSON_RPC_VERSION = "2.0";

    private final SqlMiddlewareMcpToolRouter router;
    private final ObjectMapper objectMapper;

    /**
     * Creates a server backed by tool router.
     *
     * @param router tool router
     */
    public SqlMiddlewareMcpServer(SqlMiddlewareMcpToolRouter router) {
        this(router, new ObjectMapper());
    }

    SqlMiddlewareMcpServer(SqlMiddlewareMcpToolRouter router, ObjectMapper objectMapper) {
        this.router = Objects.requireNonNull(router, "router must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    /**
     * Starts serving MCP requests until EOF or explicit {@code exit} notification.
     *
     * @param input  input stream for framed JSON-RPC requests
     * @param output output stream for framed JSON-RPC responses
     * @throws IOException if I/O fails
     */
    public void serve(InputStream input, OutputStream output) throws IOException {
        Objects.requireNonNull(input, "input must not be null");
        Objects.requireNonNull(output, "output must not be null");

        var in = new BufferedInputStream(input);
        var out = new BufferedOutputStream(output);
        var running = true;

        while (running) {
            var message = readFramedMessage(in);
            if (message == null) {
                break;
            }

            JsonNode request;
            try {
                request = objectMapper.readTree(message);
            }
            catch (Exception ex) {
                writeError(out, null, -32700, "Parse error: " + ex.getMessage());
                continue;
            }

            var method = textOrNull(request.get("method"));
            var id = request.get("id");
            var params = request.get("params");

            if (method == null) {
                if (id != null) {
                    writeError(out, id, -32600, "Invalid Request: missing method");
                }
                continue;
            }

            try {
                switch (method) {
                    case "initialize" -> writeResult(out, id, initializeResult());
                    case "tools/list" -> writeResult(out, id, toolsListResult());
                    case "tools/call" -> writeResult(out, id, handleToolsCall(params));
                    case "shutdown" -> writeResult(out, id, null);
                    case "exit" -> running = false;
                    default -> {
                        if (id != null) {
                            writeError(out, id, -32601, "Method not found: " + method);
                        }
                    }
                }
            }
            catch (Exception ex) {
                if (id != null) {
                    writeError(out, id, -32000, ex.getMessage());
                }
            }
        }
    }

    private Object initializeResult() {
        return Map.of(
            "protocolVersion", "2024-11-05",
            "capabilities", Map.of(
                "tools", Map.of("listChanged", false)
            ),
            "serverInfo", Map.of(
                "name", "sqm-middleware-mcp",
                "version", "0.3.0-SNAPSHOT"
            )
        );
    }

    private Object toolsListResult() {
        return Map.of(
            "tools", List.of(
                tool(SqlMiddlewareMcpToolRouter.ANALYZE_TOOL, "Analyze SQL and return middleware decision."),
                tool(SqlMiddlewareMcpToolRouter.ENFORCE_TOOL, "Enforce policies for execution-intent SQL."),
                tool(SqlMiddlewareMcpToolRouter.EXPLAIN_TOOL, "Explain middleware decision for SQL.")
            )
        );
    }

    private Map<String, Object> tool(String name, String description) {
        return Map.of(
            "name", name,
            "description", description,
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "sql", Map.of("type", "string"),
                    "context", Map.of("type", "object")
                ),
                "required", List.of("sql", "context")
            )
        );
    }

    private Object handleToolsCall(JsonNode params) {
        if (params == null || params.isNull()) {
            throw new IllegalArgumentException("tools/call requires params");
        }

        var toolName = textOrNull(params.get("name"));
        var arguments = params.get("arguments");

        if (toolName == null) {
            throw new IllegalArgumentException("tools/call requires params.name");
        }
        if (arguments == null || arguments.isNull()) {
            throw new IllegalArgumentException("tools/call requires params.arguments");
        }

        Object payload = switch (toolName) {
            case SqlMiddlewareMcpToolRouter.ANALYZE_TOOL -> objectMapper.convertValue(arguments, AnalyzeRequest.class);
            case SqlMiddlewareMcpToolRouter.ENFORCE_TOOL -> objectMapper.convertValue(arguments, EnforceRequest.class);
            case SqlMiddlewareMcpToolRouter.EXPLAIN_TOOL -> objectMapper.convertValue(arguments, ExplainRequest.class);
            default -> throw new IllegalArgumentException("Unsupported MCP tool: " + toolName);
        };

        var result = router.invoke(toolName, payload);
        String text;
        try {
            text = objectMapper.writeValueAsString(result);
        }
        catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize tool result", ex);
        }
        return Map.of(
            "content", List.of(Map.of("type", "text", "text", text)),
            "structuredContent", result
        );
    }

    private void writeResult(OutputStream out, JsonNode id, Object result) throws IOException {
        var response = new LinkedHashMap<String, Object>();
        response.put("jsonrpc", JSON_RPC_VERSION);
        response.put("id", id == null ? null : objectMapper.treeToValue(id, Object.class));
        response.put("result", result);
        writeFramedMessage(out, objectMapper.writeValueAsBytes(response));
    }

    private void writeError(OutputStream out, JsonNode id, int code, String message) throws IOException {
        var response = new LinkedHashMap<String, Object>();
        response.put("jsonrpc", JSON_RPC_VERSION);
        response.put("id", id == null ? null : objectMapper.treeToValue(id, Object.class));
        response.put("error", Map.of(
            "code", code,
            "message", message
        ));
        writeFramedMessage(out, objectMapper.writeValueAsBytes(response));
    }

    private void writeFramedMessage(OutputStream out, byte[] json) throws IOException {
        var header = "Content-Length: " + json.length + "\r\n\r\n";
        out.write(header.getBytes(StandardCharsets.UTF_8));
        out.write(json);
        out.flush();
    }

    private String readFramedMessage(InputStream in) throws IOException {
        var contentLength = -1;

        while (true) {
            var line = readHeaderLine(in);
            if (line == null) {
                return null;
            }

            if (line.isEmpty()) {
                break;
            }

            var lower = line.toLowerCase();
            if (lower.startsWith("content-length:")) {
                var value = line.substring("Content-Length:".length()).trim();
                contentLength = Integer.parseInt(value);
            }
        }

        if (contentLength < 0) {
            throw new IOException("Missing Content-Length header");
        }

        var body = in.readNBytes(contentLength);
        if (body.length != contentLength) {
            throw new IOException("Unexpected EOF while reading framed message body");
        }
        return new String(body, StandardCharsets.UTF_8);
    }

    private String readHeaderLine(InputStream in) throws IOException {
        var bytes = new java.io.ByteArrayOutputStream();
        int previous = -1;

        while (true) {
            var current = in.read();
            if (current == -1) {
                if (bytes.size() == 0) {
                    return null;
                }
                break;
            }

            if (previous == '\r' && current == '\n') {
                var data = bytes.toByteArray();
                return new String(data, 0, Math.max(0, data.length - 1), StandardCharsets.UTF_8);
            }

            bytes.write(current);
            previous = current;
        }

        return bytes.toString(StandardCharsets.UTF_8);
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asText();
    }
}
