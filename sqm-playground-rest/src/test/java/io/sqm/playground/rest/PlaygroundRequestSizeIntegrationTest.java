package io.sqm.playground.rest;

import io.sqm.playground.rest.error.PlaygroundErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration tests for playground request-size guardrails.
 */
@SpringBootTest(
    classes = SqmPlaygroundRestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "sqm.playground.rest.abuse.rate-limit-enabled=false",
        "sqm.playground.rest.abuse.max-request-bytes=1"
    }
)
class PlaygroundRequestSizeIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * Verifies that oversized request is rejected with stable 413 response.
     */
    @Test
    void rejectsOversizedRequest() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var response = restTemplate.postForEntity(
            "http://localhost:" + port + PlaygroundApiPaths.BASE_PATH + "/parse",
            new HttpEntity<>("{\"sql\":\"select 1\",\"sourceDialect\":\"ansi\"}", headers),
            PlaygroundErrorResponse.class
        );

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("REQUEST_TOO_LARGE", response.getBody().code());
    }
}
