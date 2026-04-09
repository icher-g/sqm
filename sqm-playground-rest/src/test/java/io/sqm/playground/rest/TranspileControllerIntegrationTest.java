package io.sqm.playground.rest;

import io.sqm.playground.api.SqlDialectDto;
import io.sqm.playground.api.TranspileOutcomeDto;
import io.sqm.playground.api.TranspileRequestDto;
import io.sqm.playground.api.TranspileResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for playground transpile endpoint.
 */
@SpringBootTest(
    classes = SqmPlaygroundRestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class TranspileControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void transpileEndpointReturnsExactRewrite() {
        var response = restTemplate.postForEntity(
            "http://localhost:" + port + PlaygroundApiPaths.BASE_PATH + "/transpile",
            new HttpEntity<>(new TranspileRequestDto(
                "select first_name || ' ' || last_name as full_name from users",
                SqlDialectDto.postgresql,
                SqlDialectDto.mysql
            )),
            TranspileResponseDto.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertEquals(TranspileOutcomeDto.exact, response.getBody().outcome());
        assertNotNull(response.getBody().renderedSql());
    }

    @Test
    void transpileEndpointReturnsWarningsForApproximateRewrite() {
        var response = restTemplate.postForEntity(
            "http://localhost:" + port + PlaygroundApiPaths.BASE_PATH + "/transpile",
            new HttpEntity<>(new TranspileRequestDto(
                "select * from users where name ilike 'al%'",
                SqlDialectDto.postgresql,
                SqlDialectDto.mysql
            )),
            TranspileResponseDto.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertEquals(TranspileOutcomeDto.approximate, response.getBody().outcome());
        assertFalse(response.getBody().diagnostics().isEmpty());
    }
}
