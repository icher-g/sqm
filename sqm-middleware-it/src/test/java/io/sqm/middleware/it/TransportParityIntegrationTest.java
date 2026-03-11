package io.sqm.middleware.it;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.control.config.SqlDecisionServiceConfig;
import io.sqm.middleware.api.AnalyzeRequest;
import io.sqm.middleware.api.DecisionExplanationDto;
import io.sqm.middleware.api.DecisionResultDto;
import io.sqm.middleware.api.EnforceRequest;
import io.sqm.middleware.api.ExecutionContextDto;
import io.sqm.middleware.api.ExplainRequest;
import io.sqm.middleware.core.SqlMiddlewareServices;
import io.sqm.middleware.mcp.SqlMiddlewareMcpAdapter;
import io.sqm.middleware.mcp.SqlMiddlewareMcpToolRouter;
import io.sqm.middleware.rest.adapter.SqlMiddlewareRestAdapter;
import io.sqm.middleware.rest.controller.SqlMiddlewareRestController;
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

    @Test
    void rest_and_mcp_transports_match_core_service_for_mysql_dml_requests() {
        var service = SqlMiddlewareServices.create(
            SqlDecisionServiceConfig.builder(SCHEMA)
                .buildValidationAndRewriteConfig()
        );

        var restController = new SqlMiddlewareRestController(new SqlMiddlewareRestAdapter(service));
        var mcpRouter = new SqlMiddlewareMcpToolRouter(new SqlMiddlewareMcpAdapter(service));
        var context = new ExecutionContextDto("mysql", null, null, null, null);
        var enforceRequest = new EnforceRequest(
            "insert ignore into users (id, name) values (1, 'alice')",
            context
        );
        var explainRequest = new ExplainRequest(
            "update users set name = 'alice' where id = 1",
            context
        );

        var directEnforce = service.enforce(enforceRequest);
        var restEnforce = restController.enforce(enforceRequest);
        var mcpEnforce = (DecisionResultDto) mcpRouter.invoke(SqlMiddlewareMcpToolRouter.ENFORCE_TOOL, enforceRequest);

        assertEquals(directEnforce.kind(), restEnforce.kind());
        assertEquals(directEnforce.reasonCode(), restEnforce.reasonCode());
        assertEquals(directEnforce.kind(), mcpEnforce.kind());
        assertEquals(directEnforce.reasonCode(), mcpEnforce.reasonCode());

        var directExplain = service.explainDecision(explainRequest);
        var restExplain = restController.explain(explainRequest);
        var mcpExplain = (DecisionExplanationDto) mcpRouter.invoke(SqlMiddlewareMcpToolRouter.EXPLAIN_TOOL, explainRequest);

        assertEquals(directExplain.decision().kind(), restExplain.decision().kind());
        assertEquals(directExplain.decision().kind(), mcpExplain.decision().kind());
        assertEquals(directExplain.decision().reasonCode(), restExplain.decision().reasonCode());
        assertEquals(directExplain.decision().reasonCode(), mcpExplain.decision().reasonCode());
    }
}

