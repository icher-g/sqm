package io.sqm.middleware.it;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.control.config.SqlDecisionServiceConfig;
import io.sqm.middleware.api.AnalyzeRequest;
import io.sqm.middleware.api.DecisionExplanationDto;
import io.sqm.middleware.api.DecisionKindDto;
import io.sqm.middleware.api.DecisionResultDto;
import io.sqm.middleware.api.EnforceRequest;
import io.sqm.middleware.api.ExecutionContextDto;
import io.sqm.middleware.api.ExplainRequest;
import io.sqm.middleware.api.ReasonCodeDto;
import io.sqm.middleware.core.SqlMiddlewareServices;
import io.sqm.middleware.mcp.SqlMiddlewareMcpAdapter;
import io.sqm.middleware.mcp.SqlMiddlewareMcpToolRouter;
import io.sqm.middleware.rest.adapter.SqlMiddlewareRestAdapter;
import io.sqm.middleware.rest.controller.SqlMiddlewareRestController;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransportParityIntegrationTest {
    @Test
    void oracleModelAndItsValidationMessageSurviveAllTransports() {
        var service = SqlMiddlewareServices.create(SqlDecisionServiceConfig.builder(SCHEMA).buildValidationAndRewriteConfig());
        var rest = new SqlMiddlewareRestController(new SqlMiddlewareRestAdapter(service));
        var mcp = new SqlMiddlewareMcpToolRouter(new SqlMiddlewareMcpAdapter(service));
        var context = new ExecutionContextDto("oracle", null, null, null, null);
        var sql = "SELECT name FROM users MODEL DIMENSION BY (id) MEASURES (name) (name[1] = 'updated')";
        var request = new AnalyzeRequest(sql, context);
        var result = service.analyze(request);
        assertEquals(DecisionKindDto.REWRITE, result.kind(), result::message);
        assertTrue(result.rewrittenSql().contains("MODEL"));
        assertTrue(result.rewrittenSql().contains("name[1] = 'updated'"));
        assertEquals(result.rewrittenSql(), rest.analyze(request).rewrittenSql());
        assertEquals(result.rewrittenSql(), ((DecisionResultDto) mcp.invoke(SqlMiddlewareMcpToolRouter.ANALYZE_TOOL, request)).rewrittenSql());
        var invalid = new AnalyzeRequest("SELECT name FROM users MODEL DIMENSION BY (id) MEASURES (name) (name[1] = PREVIOUS(name[1]))", context);
        var denied = service.analyze(invalid);
        assertEquals(DecisionKindDto.DENY, denied.kind());
        assertEquals(ReasonCodeDto.DENY_UNSUPPORTED_DIALECT_FEATURE, denied.reasonCode());
        assertTrue(denied.message().contains("PREVIOUS"));
        assertEquals(denied.message(), rest.analyze(invalid).message());
        assertEquals(denied.message(), ((DecisionResultDto) mcp.invoke(SqlMiddlewareMcpToolRouter.ANALYZE_TOOL, invalid)).message());
    }

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
    void rest_and_mcp_transports_match_core_service_for_multi_statement_script() {
        var service = SqlMiddlewareServices.create(
            SqlDecisionServiceConfig.builder(SCHEMA)
                .buildValidationAndRewriteConfig()
        );

        var restController = new SqlMiddlewareRestController(new SqlMiddlewareRestAdapter(service));
        var mcpRouter = new SqlMiddlewareMcpToolRouter(new SqlMiddlewareMcpAdapter(service));
        var request = new AnalyzeRequest(
            "select id from users; select name from users;",
            new ExecutionContextDto("postgresql", null, null, null, null)
        );

        var direct = service.analyze(request);
        var rest = restController.analyze(request);
        var mcp = (DecisionResultDto) mcpRouter.invoke(SqlMiddlewareMcpToolRouter.ANALYZE_TOOL, request);

        assertEquals(DecisionKindDto.REWRITE, direct.kind());
        assertTrue(direct.rewrittenSql() != null && direct.rewrittenSql().contains(";"));
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

    @Test
    void rest_and_mcp_transports_match_core_service_for_sqlserver_query_and_dml_requests() {
        var service = SqlMiddlewareServices.create(
            SqlDecisionServiceConfig.builder(SCHEMA)
                .buildValidationAndRewriteConfig()
        );

        var restController = new SqlMiddlewareRestController(new SqlMiddlewareRestAdapter(service));
        var mcpRouter = new SqlMiddlewareMcpToolRouter(new SqlMiddlewareMcpAdapter(service));
        var context = new ExecutionContextDto("sqlserver", null, null, null, null);
        var analyzeRequest = new AnalyzeRequest(
            "select top (5) [id] from [users] order by [id]",
            context
        );
        var enforceRequest = new EnforceRequest(
            "delete from [users] where [id] = 1",
            context
        );

        var directAnalyze = service.analyze(analyzeRequest);
        var restAnalyze = restController.analyze(analyzeRequest);
        var mcpAnalyze = (DecisionResultDto) mcpRouter.invoke(SqlMiddlewareMcpToolRouter.ANALYZE_TOOL, analyzeRequest);

        assertEquals(directAnalyze.kind(), restAnalyze.kind());
        assertEquals(directAnalyze.reasonCode(), restAnalyze.reasonCode());
        assertEquals(directAnalyze.kind(), mcpAnalyze.kind());
        assertEquals(directAnalyze.reasonCode(), mcpAnalyze.reasonCode());

        var directEnforce = service.enforce(enforceRequest);
        var restEnforce = restController.enforce(enforceRequest);
        var mcpEnforce = (DecisionResultDto) mcpRouter.invoke(SqlMiddlewareMcpToolRouter.ENFORCE_TOOL, enforceRequest);

        assertEquals(directEnforce.kind(), restEnforce.kind());
        assertEquals(directEnforce.reasonCode(), restEnforce.reasonCode());
        assertEquals(directEnforce.kind(), mcpEnforce.kind());
        assertEquals(directEnforce.reasonCode(), mcpEnforce.reasonCode());
    }

    @Test
    void rest_and_mcp_transports_match_core_service_for_sqlserver_advanced_requests() {
        var service = SqlMiddlewareServices.create(
            SqlDecisionServiceConfig.builder(SCHEMA)
                .buildValidationAndRewriteConfig()
        );

        var restController = new SqlMiddlewareRestController(new SqlMiddlewareRestAdapter(service));
        var mcpRouter = new SqlMiddlewareMcpToolRouter(new SqlMiddlewareMcpAdapter(service));
        var context = new ExecutionContextDto("sqlserver", null, null, null, null);
        var analyzeRequest = new AnalyzeRequest(
            "select top (10) percent [u].[id] from [users] as [u] with (nolock) order by [u].[id]",
            context
        );
        var enforceRequest = new EnforceRequest(
            """
                merge top (10) percent into [users] with (holdlock)
                using [users] as [s]
                on [users].[id] = [s].[id]
                when matched and [s].[id] = 1 then update set [name] = [s].[name]
                when not matched by source and [users].[id] = 1 then delete
                output deleted.[id]
                """,
            context
        );

        var directAnalyze = service.analyze(analyzeRequest);
        var restAnalyze = restController.analyze(analyzeRequest);
        var mcpAnalyze = (DecisionResultDto) mcpRouter.invoke(SqlMiddlewareMcpToolRouter.ANALYZE_TOOL, analyzeRequest);

        assertEquals(directAnalyze.kind(), restAnalyze.kind());
        assertEquals(directAnalyze.reasonCode(), restAnalyze.reasonCode());
        assertEquals(directAnalyze.kind(), mcpAnalyze.kind());
        assertEquals(directAnalyze.reasonCode(), mcpAnalyze.reasonCode());

        var directEnforce = service.enforce(enforceRequest);
        var restEnforce = restController.enforce(enforceRequest);
        var mcpEnforce = (DecisionResultDto) mcpRouter.invoke(SqlMiddlewareMcpToolRouter.ENFORCE_TOOL, enforceRequest);

        assertEquals(directEnforce.kind(), restEnforce.kind());
        assertEquals(directEnforce.reasonCode(), restEnforce.reasonCode());
        assertEquals(directEnforce.kind(), mcpEnforce.kind());
        assertEquals(directEnforce.reasonCode(), mcpEnforce.reasonCode());
    }

    @Test
    void invalid_sqlserver_advanced_top_surfaces_clear_diagnostics_across_transports() {
        var service = SqlMiddlewareServices.create(
            SqlDecisionServiceConfig.builder(SCHEMA)
                .buildValidationAndRewriteConfig()
        );

        var restController = new SqlMiddlewareRestController(new SqlMiddlewareRestAdapter(service));
        var mcpRouter = new SqlMiddlewareMcpToolRouter(new SqlMiddlewareMcpAdapter(service));
        var request = new AnalyzeRequest(
            "select top 10 with ties [id] from [users]",
            new ExecutionContextDto("sqlserver", null, null, null, null)
        );

        var direct = service.analyze(request);
        var rest = restController.analyze(request);
        var mcp = (DecisionResultDto) mcpRouter.invoke(SqlMiddlewareMcpToolRouter.ANALYZE_TOOL, request);

        assertEquals(direct.kind(), rest.kind());
        assertEquals(direct.reasonCode(), rest.reasonCode());
        assertEquals(direct.kind(), mcp.kind());
        assertEquals(direct.reasonCode(), mcp.reasonCode());
        assertEquals(DecisionKindDto.DENY, direct.kind());
        assertEquals(ReasonCodeDto.DENY_PIPELINE_ERROR, direct.reasonCode());
        assertTrue(direct.message() != null && direct.message().toUpperCase().contains("WITH TIES"));
        assertTrue(direct.message().toUpperCase().contains("ORDER BY"));
    }
}

