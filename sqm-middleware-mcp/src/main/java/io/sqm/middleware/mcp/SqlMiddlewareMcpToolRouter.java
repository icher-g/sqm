package io.sqm.middleware.mcp;

import io.sqm.middleware.api.AnalyzeRequest;
import io.sqm.middleware.api.DecisionExplanationDto;
import io.sqm.middleware.api.DecisionResultDto;
import io.sqm.middleware.api.EnforceRequest;
import io.sqm.middleware.api.ExplainRequest;

import java.util.Objects;

/**
 * Tool-name based MCP router for SQL middleware operations.
 */
public final class SqlMiddlewareMcpToolRouter {

    /**
     * Tool name for analyze requests.
     */
    public static final String ANALYZE_TOOL = "middleware.analyze";

    /**
     * Tool name for enforce requests.
     */
    public static final String ENFORCE_TOOL = "middleware.enforce";

    /**
     * Tool name for explain requests.
     */
    public static final String EXPLAIN_TOOL = "middleware.explain";

    private final SqlMiddlewareMcpAdapter adapter;

    /**
     * Creates a router backed by MCP adapter.
     *
     * @param adapter MCP adapter
     */
    public SqlMiddlewareMcpToolRouter(SqlMiddlewareMcpAdapter adapter) {
        this.adapter = Objects.requireNonNull(adapter, "adapter must not be null");
    }

    /**
     * Dispatches a tool request by tool name and payload type.
     *
     * @param toolName tool name
     * @param request  typed request payload
     * @return decision result or decision explanation depending on tool
     */
    public Object invoke(String toolName, Object request) {
        Objects.requireNonNull(toolName, "toolName must not be null");

        return switch (toolName) {
            case ANALYZE_TOOL -> handleAnalyze(request);
            case ENFORCE_TOOL -> handleEnforce(request);
            case EXPLAIN_TOOL -> handleExplain(request);
            default -> throw new IllegalArgumentException("Unsupported MCP tool: " + toolName);
        };
    }

    private DecisionResultDto handleAnalyze(Object request) {
        if (!(request instanceof AnalyzeRequest analyzeRequest)) {
            throw new IllegalArgumentException("Request for tool " + ANALYZE_TOOL + " must be AnalyzeRequest");
        }
        return adapter.analyze(analyzeRequest);
    }

    private DecisionResultDto handleEnforce(Object request) {
        if (!(request instanceof EnforceRequest enforceRequest)) {
            throw new IllegalArgumentException("Request for tool " + ENFORCE_TOOL + " must be EnforceRequest");
        }
        return adapter.enforce(enforceRequest);
    }

    private DecisionExplanationDto handleExplain(Object request) {
        if (!(request instanceof ExplainRequest explainRequest)) {
            throw new IllegalArgumentException("Request for tool " + EXPLAIN_TOOL + " must be ExplainRequest");
        }
        return adapter.explain(explainRequest);
    }
}
