package io.sqm.playground.rest;

import io.sqm.playground.api.RenderRequestDto;
import io.sqm.playground.api.RenderResponseDto;
import io.sqm.playground.api.SqlDialectDto;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for playground render endpoint.
 */
@SpringBootTest(
    classes = SqmPlaygroundRestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class RenderControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void renderEndpointReturnsRenderedSqlForValidSql() {
        var response = restTemplate.postForEntity(
            "http://localhost:" + port + PlaygroundApiPaths.BASE_PATH + "/render",
            new HttpEntity<>(new RenderRequestDto(
                "select id, name from customer",
                SqlDialectDto.ansi,
                SqlDialectDto.postgresql
            )),
            RenderResponseDto.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertNotNull(response.getBody().renderedSql());
        assertTrue(response.getBody().diagnostics().isEmpty());
    }

    @Test
    void renderEndpointReturnsMultilineFormattingForUpdateSql() {
        var response = restTemplate.postForEntity(
            "http://localhost:" + port + PlaygroundApiPaths.BASE_PATH + "/render",
            new HttpEntity<>(new RenderRequestDto(
                "update orders o join customer c on c.id = o.customer_id set o.status = 'priority' where c.vip = 1",
                SqlDialectDto.mysql,
                SqlDialectDto.mysql
            )),
            RenderResponseDto.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertNotNull(response.getBody().renderedSql());
        assertTrue(response.getBody().renderedSql().contains("\nINNER JOIN customer AS c ON c.id = o.customer_id"));
        assertTrue(response.getBody().renderedSql().contains("\nSET o.status = 'priority'"));
        assertTrue(response.getBody().renderedSql().contains("\nWHERE c.vip = 1"));
    }

    @Test
    void renderEndpointReturnsDiagnosticsForInvalidSql() {
        var response = restTemplate.postForEntity(
            "http://localhost:" + port + PlaygroundApiPaths.BASE_PATH + "/render",
            new HttpEntity<>(new RenderRequestDto(
                "select from",
                SqlDialectDto.ansi,
                SqlDialectDto.sqlserver
            )),
            RenderResponseDto.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertNull(response.getBody().renderedSql());
        assertFalse(response.getBody().diagnostics().isEmpty());
    }
}
