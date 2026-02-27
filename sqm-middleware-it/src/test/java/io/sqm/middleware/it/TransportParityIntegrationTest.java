package io.sqm.middleware.it;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.control.SqlDecisionServiceConfig;
import io.sqm.middleware.api.AnalyzeRequest;
import io.sqm.middleware.api.DecisionResultDto;
import io.sqm.middleware.api.ExecutionContextDto;
import io.sqm.middleware.core.SqlMiddlewareServices;
import io.sqm.middleware.mcp.SqlMiddlewareMcpAdapter;
import io.sqm.middleware.mcp.SqlMiddlewareMcpToolRouter;
import io.sqm.middleware.rest.SqlMiddlewareRestAdapter;
import io.sqm.middleware.rest.SqlMiddlewareRestController;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransportParityIntegrationTest {

    private static final CatalogSchema SCHEMA = CatalogSchema.of(
        CatalogTable.of("public", "users",
            CatalogColumn.of("id", CatalogType.LONG),
            CatalogColumn.of("name", CatalogType.STRING)
        )
    );

    @Test
    void rest_and_mcp_transports_match_core_service_for_same_request() {
        var service = SqlMiddlewareServices.create(
            SqlDecisionServiceConfig.builder(SCHEMA)
                .buildValidationAndRewriteConfig()
        );

        var restController = new SqlMiddlewareRestController(new SqlMiddlewareRestAdapter(service));
        var mcpRouter = new SqlMiddlewareMcpToolRouter(new SqlMiddlewareMcpAdapter(service));

        var request = new AnalyzeRequest("select id from users", new ExecutionContextDto("postgresql", null, null, null, null));

        var direct = service.analyze(request);
        var rest = restController.analyze(request);
        var mcp = (DecisionResultDto) mcpRouter.invoke(SqlMiddlewareMcpToolRouter.ANALYZE_TOOL, request);

        assertEquals(direct.kind(), rest.kind());
        assertEquals(direct.reasonCode(), rest.reasonCode());
        assertEquals(direct.rewrittenSql(), rest.rewrittenSql());

        assertEquals(direct.kind(), mcp.kind());
        assertEquals(direct.reasonCode(), mcp.reasonCode());
        assertEquals(direct.rewrittenSql(), mcp.rewrittenSql());
    }
}
