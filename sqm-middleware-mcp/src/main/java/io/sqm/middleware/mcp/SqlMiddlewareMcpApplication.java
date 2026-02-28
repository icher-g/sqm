package io.sqm.middleware.mcp;

import io.sqm.control.ConfigKeys;
import io.sqm.middleware.core.SqlMiddlewareRuntimeFactory;

import java.io.IOException;
import java.util.Locale;

/**
 * Standalone long-running MCP host entry point.
 */
public final class SqlMiddlewareMcpApplication {

    private SqlMiddlewareMcpApplication() {
    }

    /**
     * Runs long-running MCP stdio server loop.
     *
     * @param args process arguments
     */
    public static void main(String[] args) {
        var runtime = SqlMiddlewareRuntimeFactory.createRuntimeFromEnvironment();
        var service = runtime.service();
        var adapter = new SqlMiddlewareMcpAdapter(service);
        var router = new SqlMiddlewareMcpToolRouter(adapter);
        var server = new SqlMiddlewareMcpServer(router, new com.fasterxml.jackson.databind.ObjectMapper(), readOptions());
        System.err.printf(
            "SQM MCP runtime schema bootstrap: state=%s source=%s details=%s error=%s%n", runtime.schemaBootstrapStatus().state().name(),
            runtime.schemaBootstrapStatus().source(),
            runtime.schemaBootstrapStatus().description(),
            runtime.schemaBootstrapStatus().error()
        );
        try {
            server.serve(System.in, System.out);
        }
        catch (IOException ex) {
            throw new IllegalStateException("Failed to run MCP stdio server", ex);
        }
    }

    private static SqlMiddlewareMcpServerOptions readOptions() {
        var defaults = SqlMiddlewareMcpServerOptions.defaults();
        return new SqlMiddlewareMcpServerOptions(
            readInt(ConfigKeys.MCP_MAX_CONTENT_LENGTH_BYTES, defaults.maxContentLengthBytes()),
            readInt(ConfigKeys.MCP_MAX_HEADER_LINE_LENGTH_BYTES, defaults.maxHeaderLineLengthBytes()),
            readInt(ConfigKeys.MCP_MAX_HEADER_BYTES, defaults.maxHeaderBytes()),
            readBoolean(ConfigKeys.MCP_REQUIRE_INITIALIZE_BEFORE_TOOLS, defaults.requireInitializeBeforeTools())
        );
    }

    private static int readInt(ConfigKeys.Key key, int defaultValue) {
        var raw = readString(key);
        return raw == null ? defaultValue : Integer.parseInt(raw);
    }

    private static boolean readBoolean(ConfigKeys.Key key, boolean defaultValue) {
        var raw = readString(key);
        return raw == null ? defaultValue : Boolean.parseBoolean(raw.toLowerCase(Locale.ROOT));
    }

    private static String readString(ConfigKeys.Key key) {
        var prop = System.getProperty(key.property());
        if (prop != null && !prop.isBlank()) {
            return prop;
        }
        var env = System.getenv(key.env());
        if (env != null && !env.isBlank()) {
            return env;
        }
        return null;
    }
}
