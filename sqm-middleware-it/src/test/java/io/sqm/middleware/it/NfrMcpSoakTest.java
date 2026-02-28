package io.sqm.middleware.it;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.control.SqlDecisionServiceConfig;
import io.sqm.middleware.api.AnalyzeRequest;
import io.sqm.middleware.api.ExecutionContextDto;
import io.sqm.middleware.api.DecisionKindDto;
import io.sqm.middleware.core.SqlMiddlewareServices;
import io.sqm.middleware.mcp.SqlMiddlewareMcpAdapter;
import io.sqm.middleware.mcp.SqlMiddlewareMcpToolRouter;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NfrMcpSoakTest {

    private static final CatalogSchema SCHEMA = CatalogSchema.of(
        CatalogTable.of("public", "users",
            CatalogColumn.of("id", CatalogType.LONG),
            CatalogColumn.of("name", CatalogType.STRING)
        )
    );

    @Test
    void mcp_tool_router_soak_remains_stable() {
        int iterations = intProp("sqm.nfr.mcp.soak.iterations", 1500);
        long maxDurationMillis = intProp("sqm.nfr.mcp.soak.max.duration.ms", 15000);

        var service = SqlMiddlewareServices.create(
            SqlDecisionServiceConfig.builder(SCHEMA).buildValidationAndRewriteConfig()
        );
        var router = new SqlMiddlewareMcpToolRouter(new SqlMiddlewareMcpAdapter(service));

        long started = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            var request = new AnalyzeRequest("select id from users", new ExecutionContextDto("postgresql", null, null, null, null));
            var decision = (io.sqm.middleware.api.DecisionResultDto) router.invoke(SqlMiddlewareMcpToolRouter.ANALYZE_TOOL, request);
            assertNotEquals(DecisionKindDto.DENY, decision.kind());
        }
        long elapsed = Duration.ofNanos(System.nanoTime() - started).toMillis();
        assertTrue(elapsed <= maxDurationMillis, "MCP soak run exceeded max duration: " + elapsed + "ms");
    }

    private static int intProp(String key, int defaultValue) {
        var raw = System.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(raw.trim());
    }
}
