package io.sqm.playground.rest;

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

/**
 * Integration tests for playground CORS configuration.
 */
@SpringBootTest(
    classes = SqmPlaygroundRestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "sqm.playground.rest.abuse.rate-limit-enabled=false",
        "sqm.playground.rest.cors.allowed-origins=http://localhost:5173"
    }
)
class PlaygroundCorsIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * Verifies that allowed frontend origins receive the configured CORS header.
     */
    @Test
    void returnsCorsHeaderForAllowedOrigin() {
        var headers = new HttpHeaders();
        headers.setOrigin("http://localhost:5173");

        var response = restTemplate.exchange(
            "http://localhost:" + port + "/api/v1/examples",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("http://localhost:5173", response.getHeaders().getAccessControlAllowOrigin());
    }
}
