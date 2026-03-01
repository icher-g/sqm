package io.sqm.middleware.rest;

import io.sqm.middleware.api.AnalyzeRequest;
import io.sqm.middleware.api.ExecutionContextDto;
import io.sqm.middleware.api.ExecutionModeDto;
import io.sqm.middleware.api.ParameterizationModeDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration tests for REST host request-size guardrails.
 */
@SpringBootTest(
    classes = SqlMiddlewareRestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "sqm.middleware.rest.security.apiKeyEnabled=false",
        "sqm.middleware.rest.abuse.rateLimitEnabled=false",
        "sqm.middleware.rest.abuse.maxRequestBytes=1"
    }
)
class SqlMiddlewareRestRequestSizeIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * Verifies that oversized request is rejected with stable 413 response.
     */
    @Test
    void rejects_oversized_request() {
        var request = new AnalyzeRequest(
            "select id from users where id = 1",
            new ExecutionContextDto("postgresql", "agent", "tenant-a", ExecutionModeDto.ANALYZE, ParameterizationModeDto.OFF)
        );

        var response = restTemplate.postForEntity(
            "http://localhost:" + port + "/sqm/middleware/v1/analyze",
            request,
            RestErrorResponse.class
        );

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("REQUEST_TOO_LARGE", response.getBody().code());
    }
}
