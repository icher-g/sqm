package io.sqm.middleware.it;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.control.execution.ExecutionContext;
import io.sqm.control.execution.ExecutionMode;
import io.sqm.control.service.SqlDecisionService;
import io.sqm.control.config.SqlDecisionServiceConfig;
import io.sqm.middleware.api.AnalyzeRequest;
import io.sqm.middleware.api.DecisionKindDto;
import io.sqm.middleware.api.EnforceRequest;
import io.sqm.middleware.api.ExecutionContextDto;
import io.sqm.middleware.core.SqlMiddlewareCoreService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class MiddlewareServiceParityTest {

    private static final CatalogSchema SCHEMA = CatalogSchema.of(
        CatalogTable.of("public", "users",
            CatalogColumn.of("id", CatalogType.LONG),
            CatalogColumn.of("name", CatalogType.STRING)
        )
    );

    @Test
    void analyze_matches_control_allow_behavior_for_simple_select() {
        var decisionService = SqlDecisionService.create(
            SqlDecisionServiceConfig.builder(SCHEMA)
                .buildValidationConfig()
        );
        var service = new SqlMiddlewareCoreService(decisionService);
        var sql = "select id from users";

        var direct = decisionService.analyze(sql, ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));
        var viaService = service.analyze(
            new AnalyzeRequest(sql, new ExecutionContextDto("postgresql", null, null, null, null))
        );

        assertEquals(direct.reasonCode().name(), viaService.reasonCode().name());
        assertEquals(DecisionKindDto.ALLOW, viaService.kind());
    }

    @Test
    void sqlserver_query_and_dml_requests_match_control_without_pipeline_failure() {
        var decisionService = SqlDecisionService.create(
            SqlDecisionServiceConfig.builder(SCHEMA)
                .buildValidationConfig()
        );
        var service = new SqlMiddlewareCoreService(decisionService);

        var analyzeSql = "select top (5) [id] from [users] order by [id]";
        var enforceSql = "update [users] set [name] = 'alice' where [id] = 1";
        var context = new ExecutionContextDto("sqlserver", null, null, null, null);

        var directAnalyze = decisionService.analyze(analyzeSql, ExecutionContext.of("sqlserver", ExecutionMode.ANALYZE));
        var viaAnalyze = service.analyze(new AnalyzeRequest(analyzeSql, context));
        var directEnforce = decisionService.enforce(enforceSql, ExecutionContext.of("sqlserver", ExecutionMode.EXECUTE));
        var viaEnforce = service.enforce(new EnforceRequest(enforceSql, context));

        assertEquals(directAnalyze.reasonCode().name(), viaAnalyze.reasonCode().name());
        assertEquals(directAnalyze.kind().name(), viaAnalyze.kind().name());
        assertNotEquals("DENY_PIPELINE_ERROR", viaAnalyze.reasonCode().name());

        assertEquals(directEnforce.reasonCode().name(), viaEnforce.reasonCode().name());
        assertEquals(directEnforce.kind().name(), viaEnforce.kind().name());
        assertNotEquals("DENY_PIPELINE_ERROR", viaEnforce.reasonCode().name());
    }
}


