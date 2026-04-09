package io.sqm.playground.rest;

import io.sqm.playground.rest.error.PlaygroundErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration tests for playground rate limiting.
 */
@SpringBootTest(
    classes = SqmPlaygroundRestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "sqm.playground.rest.abuse.rate-limit-enabled=true",
        "sqm.playground.rest.abuse.requests-per-window=1",
        "sqm.playground.rest.abuse.window-seconds=60"
    }
)
class PlaygroundRateLimitIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * Verifies that second request in same window is rejected with 429.
     */
    @Test
    void rejectsRequestAboveRateLimit() {
        var first = restTemplate.getForEntity(url("/api/v1/examples"), String.class);
        var second = restTemplate.getForEntity(url("/api/v1/examples"), PlaygroundErrorResponse.class);

        assertEquals(HttpStatus.OK, first.getStatusCode());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, second.getStatusCode());
        assertNotNull(second.getBody());
        assertEquals("RATE_LIMIT_EXCEEDED", second.getBody().code());
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
