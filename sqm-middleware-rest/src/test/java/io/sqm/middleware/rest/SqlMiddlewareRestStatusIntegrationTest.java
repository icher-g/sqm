package io.sqm.middleware.rest;

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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for runtime health/readiness status in ready startup mode.
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
class SqlMiddlewareRestStatusIntegrationTest {

    private static final String SCHEMA_SOURCE_KEY = "sqm.middleware.schema.source";
    private static final String SCHEMA_JSON_PATH_KEY = "sqm.middleware.schema.json.path";
    private static final String SCHEMA_FAIL_FAST_KEY = "sqm.middleware.schema.bootstrap.failFast";

    @BeforeAll
    static void setupSchemaBootstrapProperties() {
        System.setProperty(SCHEMA_SOURCE_KEY, "manual");
        System.clearProperty(SCHEMA_JSON_PATH_KEY);
        System.clearProperty(SCHEMA_FAIL_FAST_KEY);
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
    void health_and_readiness_are_ready_when_schema_bootstrap_succeeds() {
        var health = restTemplate.getForEntity(
            "http://localhost:" + port + "/sqm/middleware/v1/health",
            SqlMiddlewareStatusResponse.class
        );
        assertEquals(HttpStatus.OK, health.getStatusCode());
        assertNotNull(health.getBody());
        assertEquals("UP", health.getBody().status());
        assertEquals("READY", health.getBody().schemaState());
        assertTrue(health.getBody().schemaDescription().startsWith("manual"));
        assertNull(health.getBody().schemaErrorMessage());

        var readiness = restTemplate.getForEntity(
            "http://localhost:" + port + "/sqm/middleware/v1/readiness",
            SqlMiddlewareStatusResponse.class
        );
        assertEquals(HttpStatus.OK, readiness.getStatusCode());
        assertNotNull(readiness.getBody());
        assertEquals("READY", readiness.getBody().status());
        assertEquals("READY", readiness.getBody().schemaState());
        assertNull(readiness.getBody().schemaErrorMessage());
    }

}
