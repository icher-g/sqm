package io.sqm.middleware.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration tests for stable REST error contract behavior.
 */
@SpringBootTest(
    classes = SqlMiddlewareRestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "sqm.middleware.rest.security.apiKeyEnabled=false",
        "sqm.middleware.rest.abuse.rateLimitEnabled=false"
    }
)
class SqlMiddlewareRestErrorContractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * Verifies malformed JSON is returned as stable invalid-request payload.
     */
    @Test
    void malformed_json_returns_stable_error_contract() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var request = new HttpEntity<>("{\"sql\":", headers);

        var response = restTemplate.exchange(
            "http://localhost:" + port + "/sqm/middleware/v1/analyze",
            HttpMethod.POST,
            request,
            RestErrorResponse.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_REQUEST", response.getBody().code());
        assertEquals("/sqm/middleware/v1/analyze", response.getBody().path());
    }

    /**
     * Verifies missing request context is returned as stable invalid-request payload.
     */
    @Test
    void missing_context_returns_stable_error_contract() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var request = new HttpEntity<>("{\"sql\":\"select 1\"}", headers);

        var response = restTemplate.exchange(
            "http://localhost:" + port + "/sqm/middleware/v1/analyze",
            HttpMethod.POST,
            request,
            RestErrorResponse.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_REQUEST", response.getBody().code());
        assertEquals("/sqm/middleware/v1/analyze", response.getBody().path());
    }
}
