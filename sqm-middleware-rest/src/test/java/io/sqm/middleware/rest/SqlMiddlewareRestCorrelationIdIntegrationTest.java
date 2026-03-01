package io.sqm.middleware.rest;

import io.sqm.middleware.api.AnalyzeRequest;
import io.sqm.middleware.api.DecisionResultDto;
import io.sqm.middleware.api.ExecutionContextDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration tests for REST correlation id propagation.
 */
@SpringBootTest(
    classes = SqlMiddlewareRestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "sqm.middleware.rest.security.apiKeyEnabled=false",
        "sqm.middleware.rest.abuse.rateLimitEnabled=false"
    }
)
class SqlMiddlewareRestCorrelationIdIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void echoes_correlation_id_from_request_header() {
        var headers = new HttpHeaders();
        headers.add(CorrelationIdFilter.HEADER_NAME, "cid-123");
        var request = new HttpEntity<>(analyzeRequest(), headers);

        var response = restTemplate.exchange(
            url("/sqm/middleware/v1/analyze"),
            HttpMethod.POST,
            request,
            DecisionResultDto.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("cid-123", response.getHeaders().getFirst(CorrelationIdFilter.HEADER_NAME));
    }

    @Test
    void generates_correlation_id_when_missing() {
        var response = restTemplate.postForEntity(
            url("/sqm/middleware/v1/analyze"),
            analyzeRequest(),
            DecisionResultDto.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        var correlationId = response.getHeaders().getFirst(CorrelationIdFilter.HEADER_NAME);
        assertNotNull(correlationId);
        assertFalse(correlationId.isBlank());
    }

    private AnalyzeRequest analyzeRequest() {
        return new AnalyzeRequest(
            "select 1",
            new ExecutionContextDto("postgresql", "agent", "tenant-a", null, null)
        );
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
