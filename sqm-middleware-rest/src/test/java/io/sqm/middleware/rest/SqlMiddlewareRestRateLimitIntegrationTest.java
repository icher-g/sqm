package io.sqm.middleware.rest;

import io.sqm.middleware.api.AnalyzeRequest;
import io.sqm.middleware.api.DecisionResultDto;
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
 * Integration tests for REST host rate limiting.
 */
@SpringBootTest(
    classes = SqlMiddlewareRestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "sqm.middleware.rest.security.apiKeyEnabled=false",
        "sqm.middleware.rest.abuse.rateLimitEnabled=true",
        "sqm.middleware.rest.abuse.requestsPerWindow=1",
        "sqm.middleware.rest.abuse.windowSeconds=60"
    }
)
class SqlMiddlewareRestRateLimitIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * Verifies that second request in same window is rejected with 429.
     */
    @Test
    void rejects_request_above_rate_limit() {
        var request = analyzeRequest();

        var first = restTemplate.postForEntity(url("/sqm/middleware/analyze"), request, DecisionResultDto.class);
        var second = restTemplate.postForEntity(url("/sqm/middleware/analyze"), request, RestErrorResponse.class);

        assertEquals(HttpStatus.OK, first.getStatusCode());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, second.getStatusCode());
        assertNotNull(second.getBody());
        assertEquals("RATE_LIMIT_EXCEEDED", second.getBody().code());
    }

    private AnalyzeRequest analyzeRequest() {
        return new AnalyzeRequest(
            "select 1",
            new ExecutionContextDto("postgresql", "agent", "tenant-a", ExecutionModeDto.ANALYZE, ParameterizationModeDto.OFF)
        );
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
