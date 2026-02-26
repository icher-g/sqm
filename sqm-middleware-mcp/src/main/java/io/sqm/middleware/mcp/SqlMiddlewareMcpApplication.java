package io.sqm.middleware.mcp;

import io.sqm.middleware.core.SqlMiddlewareRuntimeFactory;

import java.io.IOException;

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
        var service = SqlMiddlewareRuntimeFactory.createFromEnvironment();
        var adapter = new SqlMiddlewareMcpAdapter(service);
        var router = new SqlMiddlewareMcpToolRouter(adapter);
        var server = new SqlMiddlewareMcpServer(router);
        try {
            server.serve(System.in, System.out);
        }
        catch (IOException ex) {
            throw new IllegalStateException("Failed to run MCP stdio server", ex);
        }
    }
}
