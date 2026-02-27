package io.sqm.middleware.it;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.control.ExecutionContext;
import io.sqm.control.ExecutionMode;
import io.sqm.control.SqlDecisionService;
import io.sqm.control.SqlDecisionServiceConfig;
import io.sqm.middleware.api.AnalyzeRequest;
import io.sqm.middleware.api.DecisionKindDto;
import io.sqm.middleware.api.ExecutionContextDto;
import io.sqm.middleware.core.SqlMiddlewareCoreService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}

