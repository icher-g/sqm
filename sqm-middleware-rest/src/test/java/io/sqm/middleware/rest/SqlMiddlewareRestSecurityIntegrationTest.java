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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration tests for REST host API-key authentication.
 */
@SpringBootTest(
    classes = SqlMiddlewareRestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "sqm.middleware.rest.security.apiKeyEnabled=true",
        "sqm.middleware.rest.security.apiKeyHeader=X-API-Key",
        "sqm.middleware.rest.security.apiKeys=secret-key",
        "sqm.middleware.rest.abuse.rateLimitEnabled=false"
    }
)
class SqlMiddlewareRestSecurityIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * Verifies that requests without API key are rejected with stable JSON contract.
     */
    @Test
    void rejects_missing_api_key_with_401() {
        var request = analyzeRequest();
        ResponseEntity<RestErrorResponse> response = restTemplate.postForEntity(
            url("/sqm/middleware/v1/analyze"),
            request,
            RestErrorResponse.class
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UNAUTHORIZED", response.getBody().code());
    }

    /**
     * Verifies that requests with valid API key are accepted.
     */
    @Test
    void accepts_valid_api_key() {
        var headers = new HttpHeaders();
        headers.add("X-API-Key", "secret-key");
        var request = new HttpEntity<>(analyzeRequest(), headers);
        ResponseEntity<DecisionResultDto> response = restTemplate.exchange(
            url("/sqm/middleware/v1/analyze"),
            HttpMethod.POST,
            request,
            DecisionResultDto.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().kind());
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
