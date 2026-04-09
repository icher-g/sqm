package io.sqm.playground.rest;

import io.sqm.playground.api.SqlDialectDto;
import io.sqm.playground.api.ValidateRequestDto;
import io.sqm.playground.api.ValidateResponseDto;
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
 * Integration tests for playground validate endpoint.
 */
@SpringBootTest(
    classes = SqmPlaygroundRestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class ValidateControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void validateEndpointIgnoresUnknownSchemaObjectsForDialectOnlyValidation() {
        var response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/validate",
            new HttpEntity<>(new ValidateRequestDto(
                "select missing_col from totally_unknown_table",
                SqlDialectDto.ansi
            )),
            ValidateResponseDto.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertTrue(response.getBody().valid());
        assertTrue(response.getBody().diagnostics().isEmpty());
    }

    @Test
    void validateEndpointReturnsDialectDiagnosticsForInvalidQuery() {
        var response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/validate",
            new HttpEntity<>(new ValidateRequestDto(
                "select distinct on (id) id, name from customer order by name",
                SqlDialectDto.postgresql
            )),
            ValidateResponseDto.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertFalse(response.getBody().valid());
        assertFalse(response.getBody().diagnostics().isEmpty());
    }
}
