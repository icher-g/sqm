package io.sqm.playground.rest;

import io.sqm.playground.api.ParseRequestDto;
import io.sqm.playground.api.ParseResponseDto;
import io.sqm.playground.api.SqlDialectDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for playground parse endpoint.
 */
@SpringBootTest(
    classes = SqmPlaygroundRestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class ParseControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void parseEndpointReturnsSqmJsonAndAstForValidSql() {
        var response = restTemplate.postForEntity(
            "http://localhost:" + port + PlaygroundApiPaths.BASE_PATH + "/parse",
            new HttpEntity<>(new ParseRequestDto("select c.id from customer c where c.id = 1", SqlDialectDto.ansi)),
            ParseResponseDto.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertEquals("query", response.getBody().statementKind());
        assertFalse(response.getBody().multiStatement());
        assertNotNull(response.getBody().sqmJson());
        assertNotNull(response.getBody().sqmDsl());
        assertTrue(response.getBody().sqmDsl().contains("public static SelectQuery getStatement()"));
        assertNotNull(response.getBody().summary());
        assertNotNull(response.getBody().ast());
        assertEquals("SelectQuery", response.getBody().ast().nodeType());
        assertTrue(response.getBody().ast().children().stream().anyMatch(slot -> Objects.equals("items", slot.slot())));
    }

    @Test
    void parseEndpointReturnsStatementSequenceForMultiStatementSql() {
        var response = restTemplate.postForEntity(
            "http://localhost:" + port + PlaygroundApiPaths.BASE_PATH + "/parse",
            new HttpEntity<>(new ParseRequestDto("select 1; select 2;", SqlDialectDto.ansi)),
            ParseResponseDto.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertTrue(response.getBody().multiStatement());
        assertEquals("sequence", response.getBody().statementKind());
        assertEquals("StatementSequence", response.getBody().summary().rootNodeType());
        assertEquals("StatementSequence", response.getBody().ast().nodeType());
        var statements = response.getBody().ast().children().stream()
            .filter(slot -> Objects.equals("statements", slot.slot()))
            .findFirst()
            .orElseThrow();
        assertEquals(2, statements.nodes().size());
    }

    @Test
    void parseEndpointReturnsDiagnosticsForInvalidSql() {
        var response = restTemplate.postForEntity(
            "http://localhost:" + port + PlaygroundApiPaths.BASE_PATH + "/parse",
            new HttpEntity<>(new ParseRequestDto("select from", SqlDialectDto.ansi)),
            ParseResponseDto.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertFalse(response.getBody().diagnostics().isEmpty());
        assertEquals(1, response.getBody().diagnostics().getFirst().line());
        assertEquals(8, response.getBody().diagnostics().getFirst().column());
    }

    @Test
    void parseEndpointReturnsLineAndColumnForMultilineErrors() {
        var response = restTemplate.postForEntity(
            "http://localhost:" + port + PlaygroundApiPaths.BASE_PATH + "/parse",
            new HttpEntity<>(new ParseRequestDto("select id\nwrite form customer", SqlDialectDto.ansi)),
            ParseResponseDto.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertFalse(response.getBody().diagnostics().isEmpty());
        assertEquals(2, response.getBody().diagnostics().getFirst().line());
        assertEquals(7, response.getBody().diagnostics().getFirst().column());
    }
}
