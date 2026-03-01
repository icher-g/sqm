package io.sqm.middleware.rest;

import io.sqm.middleware.api.AnalyzeRequest;
import io.sqm.middleware.api.DecisionResultDto;
import io.sqm.middleware.api.ExecutionContextDto;
import io.sqm.middleware.api.ReasonCodeDto;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class SqlMiddlewareRestDegradedStartupIntegrationTest {

    private static final String SCHEMA_SOURCE_KEY = "sqm.middleware.schema.source";
    private static final String SCHEMA_JSON_PATH_KEY = "sqm.middleware.schema.json.path";
    private static final String SCHEMA_FAIL_FAST_KEY = "sqm.middleware.schema.bootstrap.failFast";

    @BeforeAll
    static void setupSchemaBootstrapProperties() {
        System.setProperty(SCHEMA_SOURCE_KEY, "json");
        System.setProperty(SCHEMA_JSON_PATH_KEY, "./missing-schema-for-degraded-mode.json");
        System.setProperty(SCHEMA_FAIL_FAST_KEY, "false");
    }

    @AfterAll
    static void clearSchemaBootstrapProperties() {
        System.clearProperty(SCHEMA_SOURCE_KEY);
        System.clearProperty(SCHEMA_JSON_PATH_KEY);
        System.clearProperty(SCHEMA_FAIL_FAST_KEY);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void readiness_reports_not_ready_and_analyze_denies_with_pipeline_error() {
        var readiness = restTemplate.getForEntity(
            "http://localhost:" + port + "/sqm/middleware/v1/readiness",
            SqlMiddlewareStatusResponse.class
        );
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, readiness.getStatusCode());
        assertNotNull(readiness.getBody());
        assertEquals("NOT_READY", readiness.getBody().status());
        assertEquals("DEGRADED", readiness.getBody().schemaState());
        assertNotNull(readiness.getBody().schemaErrorMessage());

        var request = new AnalyzeRequest("select 1", new ExecutionContextDto("postgresql", null, null, null, null));
        var analyze = restTemplate.postForEntity(
            "http://localhost:" + port + "/sqm/middleware/v1/analyze",
            request,
            DecisionResultDto.class
        );
        assertEquals(HttpStatus.OK, analyze.getStatusCode());
        assertNotNull(analyze.getBody());
        assertEquals(ReasonCodeDto.DENY_PIPELINE_ERROR, analyze.getBody().reasonCode());
        assertTrue(analyze.getBody().message().contains("Schema bootstrap failed"));
    }

}
