package io.sqm.middleware.mcp;

/**
 * Runtime options for MCP stdio server protocol hardening.
 *
 * @param maxContentLengthBytes maximum accepted JSON-RPC frame body size in bytes
 * @param maxHeaderLineLengthBytes maximum accepted header line length in bytes
 * @param maxHeaderBytes maximum accepted total header size in bytes
 * @param requireInitializeBeforeTools when {@code true}, tools/list and tools/call require initialize first
 */
public record SqlMiddlewareMcpServerOptions(
    int maxContentLengthBytes,
    int maxHeaderLineLengthBytes,
    int maxHeaderBytes,
    boolean requireInitializeBeforeTools
) {

    /**
     * Creates validated MCP server options.
     *
     * @param maxContentLengthBytes maximum frame body size
     * @param maxHeaderLineLengthBytes maximum header line size
     * @param maxHeaderBytes maximum total header size
     * @param requireInitializeBeforeTools initialize gate toggle
     */
    public SqlMiddlewareMcpServerOptions {
        if (maxContentLengthBytes <= 0) {
            throw new IllegalArgumentException("maxContentLengthBytes must be > 0");
        }
        if (maxHeaderLineLengthBytes <= 0) {
            throw new IllegalArgumentException("maxHeaderLineLengthBytes must be > 0");
        }
        if (maxHeaderBytes <= 0) {
            throw new IllegalArgumentException("maxHeaderBytes must be > 0");
        }
    }

    /**
     * Returns default hardening options.
     *
     * @return default MCP server options
     */
    public static SqlMiddlewareMcpServerOptions defaults() {
        return new SqlMiddlewareMcpServerOptions(
            1024 * 1024,
            8 * 1024,
            16 * 1024,
            true
        );
    }
}
