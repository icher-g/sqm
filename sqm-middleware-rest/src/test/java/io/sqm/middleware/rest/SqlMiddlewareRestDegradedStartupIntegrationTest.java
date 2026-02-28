package io.sqm.middleware.rest;

import io.sqm.middleware.api.AnalyzeRequest;
import io.sqm.middleware.api.DecisionResultDto;
import io.sqm.middleware.api.ExecutionContextDto;
import io.sqm.middleware.api.ReasonCodeDto;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for degraded startup diagnostics when schema bootstrap fails with fail-fast disabled.
 */
@SpringBootTest(
    classes = SqlMiddlewareRestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "sqm.middleware.rest.security.apiKeyEnabled=false",
        "sqm.middleware.rest.abuse.rateLimitEnabled=false"
    }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SqlMiddlewareRestDegradedStartupIntegrationTest {

    static {
        System.setProperty("sqm.middleware.schema.source", "json");
        System.setProperty("sqm.middleware.schema.json.path", "./missing-schema-for-degraded-mode.json");
        System.setProperty("sqm.middleware.schema.bootstrap.failFast", "false");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void readiness_reports_not_ready_and_analyze_denies_with_pipeline_error() {
        var readiness = restTemplate.getForEntity(
            "http://localhost:" + port + "/sqm/middleware/readiness",
            SqlMiddlewareStatusResponse.class
        );
        assertEquals(HttpStatus.OK, readiness.getStatusCode());
        assertNotNull(readiness.getBody());
        assertEquals("NOT_READY", readiness.getBody().status());
        assertEquals("DEGRADED", readiness.getBody().schemaState());
        assertNotNull(readiness.getBody().schemaErrorMessage());

        var request = new AnalyzeRequest("select 1", new ExecutionContextDto("postgresql", null, null, null, null));
        var analyze = restTemplate.postForEntity(
            "http://localhost:" + port + "/sqm/middleware/analyze",
            request,
            DecisionResultDto.class
        );
        assertEquals(HttpStatus.OK, analyze.getStatusCode());
        assertNotNull(analyze.getBody());
        assertEquals(ReasonCodeDto.DENY_PIPELINE_ERROR, analyze.getBody().reasonCode());
        assertTrue(analyze.getBody().message().contains("Schema bootstrap failed"));
    }

    @AfterAll
    static void clearSchemaBootstrapSystemProperties() {
        System.clearProperty("sqm.middleware.schema.source");
        System.clearProperty("sqm.middleware.schema.json.path");
        System.clearProperty("sqm.middleware.schema.bootstrap.failFast");
    }
}
