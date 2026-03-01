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
    private static final int INVALID_REQUEST = -32600;
    private static final int METHOD_NOT_FOUND = -32601;
    private static final int INVALID_PARAMS = -32602;
    private static final int INTERNAL_ERROR = -32603;
    private static final int PARSE_ERROR = -32700;
    private static final int SERVER_NOT_INITIALIZED = -32002;

    private final SqlMiddlewareMcpToolRouter router;
    private final ObjectMapper objectMapper;
    private final SqlMiddlewareMcpServerOptions options;
    private final String serverVersion;

    /**
     * Creates a server backed by tool router.
     *
     * @param router tool router
     */
    public SqlMiddlewareMcpServer(SqlMiddlewareMcpToolRouter router) {
        this(router, new ObjectMapper(), SqlMiddlewareMcpServerOptions.defaults());
    }

    SqlMiddlewareMcpServer(SqlMiddlewareMcpToolRouter router, ObjectMapper objectMapper) {
        this(router, objectMapper, SqlMiddlewareMcpServerOptions.defaults());
    }

    SqlMiddlewareMcpServer(
        SqlMiddlewareMcpToolRouter router,
        ObjectMapper objectMapper,
        SqlMiddlewareMcpServerOptions options
    ) {
        this.router = Objects.requireNonNull(router, "router must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.options = Objects.requireNonNull(options, "options must not be null");
        this.serverVersion = resolveServerVersion();
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
        var initialized = false;
        var shutdownRequested = false;

        while (running) {
            String message;
            try {
                message = readFramedMessage(in);
                if (message == null) {
                    break;
                }
            } catch (FramingException ex) {
                writeError(
                    out,
                    null,
                    INVALID_REQUEST,
                    ex.getMessage(),
                    Map.of("category", "INVALID_FRAME")
                );
                if (!ex.recoverable()) {
                    break;
                }
                continue;
            }

            JsonNode request;
            try {
                request = objectMapper.readTree(message);
            }
            catch (Exception ex) {
                writeError(out, null, PARSE_ERROR, "Parse error: " + ex.getMessage());
                continue;
            }

            var invalid = validateRequestShape(request);
            if (invalid != null) {
                writeError(out, invalid.id(), INVALID_REQUEST, invalid.message());
                continue;
            }

            var method = request.path("method").asText();
            var id = request.get("id");
            var params = request.get("params");

            try {
                switch (method) {
                    case "initialize" -> {
                        initialized = true;
                        writeResult(out, id, initializeResult());
                    }
                    case "tools/list" -> {
                        if (options.requireInitializeBeforeTools() && !initialized) {
                            writeError(out, id, SERVER_NOT_INITIALIZED, "Server not initialized");
                            continue;
                        }
                        if (shutdownRequested) {
                            writeError(out, id, INVALID_REQUEST, "Server is shutting down");
                            continue;
                        }
                        writeResult(out, id, toolsListResult());
                    }
                    case "tools/call" -> {
                        if (options.requireInitializeBeforeTools() && !initialized) {
                            writeError(out, id, SERVER_NOT_INITIALIZED, "Server not initialized");
                            continue;
                        }
                        if (shutdownRequested) {
                            writeError(out, id, INVALID_REQUEST, "Server is shutting down");
                            continue;
                        }
                        writeResult(out, id, handleToolsCall(params));
                    }
                    case "shutdown" -> {
                        shutdownRequested = true;
                        writeResult(out, id, null);
                    }
                    case "exit" -> running = false;
                    default -> {
                        if (id != null) {
                            writeError(out, id, METHOD_NOT_FOUND, "Method not found: " + method);
                        }
                    }
                }
            }
            catch (Exception ex) {
                writeMappedError(out, id, ex);
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
                "version", serverVersion
            )
        );
    }

    private static String resolveServerVersion() {
        var implementationVersion = SqlMiddlewareMcpServer.class.getPackage().getImplementationVersion();
        if (implementationVersion != null && !implementationVersion.isBlank()) {
            return implementationVersion;
        }

        var systemVersion = System.getProperty("sqm.version");
        if (systemVersion != null && !systemVersion.isBlank()) {
            return systemVersion;
        }

        return "dev";
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
        writeError(out, id, code, message, null);
    }

    private void writeError(OutputStream out, JsonNode id, int code, String message, Map<String, Object> data) throws IOException {
        var response = new LinkedHashMap<String, Object>();
        response.put("jsonrpc", JSON_RPC_VERSION);
        response.put("id", id == null ? null : objectMapper.treeToValue(id, Object.class));
        var error = new LinkedHashMap<String, Object>();
        error.put("code", code);
        error.put("message", message);
        if (data != null && !data.isEmpty()) {
            error.put("data", data);
        }
        response.put("error", error);
        writeFramedMessage(out, objectMapper.writeValueAsBytes(response));
    }

    private void writeMappedError(OutputStream out, JsonNode id, Exception exception) throws IOException {
        if (id == null) {
            return;
        }

        if (exception instanceof IllegalArgumentException) {
            writeError(
                out,
                id,
                INVALID_PARAMS,
                exception.getMessage(),
                Map.of("category", "INVALID_PARAMS")
            );
            return;
        }

        writeError(
            out,
            id,
            INTERNAL_ERROR,
            "Internal error",
            Map.of(
                "category", "INTERNAL_ERROR",
                "detail", exception.getMessage() == null ? "" : exception.getMessage()
            )
        );
    }

    private void writeFramedMessage(OutputStream out, byte[] json) throws IOException {
        var header = "Content-Length: " + json.length + "\r\n\r\n";
        out.write(header.getBytes(StandardCharsets.UTF_8));
        out.write(json);
        out.flush();
    }

    private String readFramedMessage(InputStream in) throws IOException {
        var contentLength = -1;
        var headerBytes = 0;

        while (true) {
            var line = readHeaderLine(in);
            if (line == null) {
                return null;
            }
            headerBytes += line.getBytes(StandardCharsets.UTF_8).length + 2;
            if (headerBytes > options.maxHeaderBytes()) {
                throw new FramingException("Header size exceeds configured max bytes", true);
            }

            if (line.isEmpty()) {
                break;
            }

            var lower = line.toLowerCase();
            if (lower.startsWith("content-length:")) {
                var value = line.substring("Content-Length:".length()).trim();
                try {
                    contentLength = Integer.parseInt(value);
                } catch (NumberFormatException ex) {
                    throw new FramingException("Invalid Content-Length value", true);
                }
            }
        }

        if (contentLength < 0) {
            throw new FramingException("Missing Content-Length header", true);
        }
        if (contentLength > options.maxContentLengthBytes()) {
            discardBody(in, contentLength);
            throw new FramingException(
                "Content-Length exceeds configured max: " + options.maxContentLengthBytes(),
                true
            );
        }

        var body = in.readNBytes(contentLength);
        if (body.length != contentLength) {
            throw new FramingException("Unexpected EOF while reading framed message body", false);
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
            if (bytes.size() > options.maxHeaderLineLengthBytes()) {
                throw new FramingException("Header line exceeds configured max bytes", true);
            }
            previous = current;
        }

        return bytes.toString(StandardCharsets.UTF_8);
    }

    private InvalidRequest validateRequestShape(JsonNode request) {
        if (request == null || !request.isObject()) {
            return new InvalidRequest(null, "Invalid Request: payload must be an object");
        }

        var jsonrpcNode = request.get("jsonrpc");
        if (jsonrpcNode == null || !JSON_RPC_VERSION.equals(textOrNull(jsonrpcNode))) {
            return new InvalidRequest(request.get("id"), "Invalid Request: jsonrpc must be '2.0'");
        }

        var id = request.get("id");
        if (id != null && !id.isTextual() && !id.isIntegralNumber() && !id.isFloatingPointNumber() && !id.isNull()) {
            return new InvalidRequest(null, "Invalid Request: id must be string, number, or null");
        }

        var method = request.get("method");
        if (method == null || !method.isTextual() || method.asText().isBlank()) {
            return new InvalidRequest(id, "Invalid Request: missing method");
        }

        return null;
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asText();
    }

    private void discardBody(InputStream in, int length) throws IOException {
        int remaining = length;
        byte[] buffer = new byte[Math.min(8192, Math.max(1, length))];
        while (remaining > 0) {
            int read = in.read(buffer, 0, Math.min(buffer.length, remaining));
            if (read < 0) {
                throw new FramingException("Unexpected EOF while discarding oversized frame body", false);
            }
            remaining -= read;
        }
    }

    private record InvalidRequest(JsonNode id, String message) {
    }

    private static final class FramingException extends IOException {
        private final boolean recoverable;

        private FramingException(String message, boolean recoverable) {
            super(message);
            this.recoverable = recoverable;
        }

        private boolean recoverable() {
            return recoverable;
        }
    }
}
