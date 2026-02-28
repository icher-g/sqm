package io.sqm.middleware.rest;

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
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SqlMiddlewareRestStatusIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void health_and_readiness_are_ready_when_schema_bootstrap_succeeds() {
        var health = restTemplate.getForEntity(
            "http://localhost:" + port + "/sqm/middleware/health",
            SqlMiddlewareStatusResponse.class
        );
        assertEquals(HttpStatus.OK, health.getStatusCode());
        assertNotNull(health.getBody());
        assertEquals("UP", health.getBody().status());
        assertEquals("READY", health.getBody().schemaState());
        assertTrue(health.getBody().schemaDescription().startsWith("manual"));
        assertNull(health.getBody().schemaErrorMessage());

        var readiness = restTemplate.getForEntity(
            "http://localhost:" + port + "/sqm/middleware/readiness",
            SqlMiddlewareStatusResponse.class
        );
        assertEquals(HttpStatus.OK, readiness.getStatusCode());
        assertNotNull(readiness.getBody());
        assertEquals("READY", readiness.getBody().status());
        assertEquals("READY", readiness.getBody().schemaState());
        assertNull(readiness.getBody().schemaErrorMessage());
    }
}
