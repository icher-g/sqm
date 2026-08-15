package io.sqm.middleware.core;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.control.service.SqlDecisionService;
import io.sqm.control.config.SqlDecisionServiceConfig;
import io.sqm.middleware.api.AnalyzeRequest;
import io.sqm.middleware.api.DecisionKindDto;
import io.sqm.middleware.api.ExecutionContextDto;
import io.sqm.middleware.api.SqlMiddlewareService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlMiddlewareServicesTest {

    private static final CatalogSchema SCHEMA = CatalogSchema.of(
        CatalogTable.of("public", "users",
            CatalogColumn.of("id", CatalogType.LONG),
            CatalogColumn.of("name", CatalogType.STRING)
        )
    );

    @Test
    void create_from_config_returns_service() {
        var config = SqlDecisionServiceConfig.builder(SCHEMA).buildValidationConfig();

        SqlMiddlewareService service = SqlMiddlewareServices.create(config);

        assertNotNull(service);
        assertNotNull(service.analyze(new AnalyzeRequest("select id from users", new ExecutionContextDto("postgresql", null, null, null, null))));
    }

    @Test
    void create_from_config_analyzes_oracle_query() {
        var config = SqlDecisionServiceConfig.builder(SCHEMA).buildValidationConfig();
        SqlMiddlewareService service = SqlMiddlewareServices.create(config);

        var result = service.analyze(new AnalyzeRequest(
            "select id from users order by id fetch first 1 rows only",
            new ExecutionContextDto("oracle", null, null, null, null)
        ));

        assertEquals(DecisionKindDto.ALLOW, result.kind());
    }

    @Test
    void create_from_middleware_returns_service() {
        var decisionService = SqlDecisionService.create(SqlDecisionServiceConfig.builder(SCHEMA).buildValidationConfig());

        SqlMiddlewareService service = SqlMiddlewareServices.create(decisionService);

        assertNotNull(service);
    }

    @Test
    void create_from_config_throws_on_null() {
        assertThrows(NullPointerException.class, () -> SqlMiddlewareServices.create((SqlDecisionServiceConfig) null));
    }

    @Test
    void create_from_middleware_throws_on_null() {
        assertThrows(NullPointerException.class, () -> SqlMiddlewareServices.create((SqlDecisionService) null));
    }
}


