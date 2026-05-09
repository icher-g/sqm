package io.sqm.playground.rest;

import io.sqm.core.dialect.SqlDialectId;
import io.sqm.playground.rest.example.ExampleCatalog;
import io.sqm.playground.rest.service.ExampleService;
import io.sqm.transpile.SqlTranspiler;
import io.sqm.transpile.TranspileStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests built-in playground examples service behavior.
 */
class ExampleServiceTest {

    @Test
    void examplesResponseContainsBuiltInExamples() {
        var service = new ExampleService(new ExampleCatalog());

        var response = service.examples();

        assertTrue(response.success());
        assertNotNull(response.requestId());
        assertFalse(response.requestId().isBlank());
        assertEquals(10, response.examples().size());
        assertEquals("basic-select", response.examples().getFirst().id());
        assertEquals("ansi", response.examples().getFirst().dialect().name());
        assertTrue(response.examples().stream().anyMatch(example -> example.id().equals("ansi-analytics-report")));
        assertTrue(response.examples().stream().anyMatch(example -> example.id().equals("postgres-merge-returning")));
        assertTrue(response.examples().stream().anyMatch(example -> example.id().equals("mysql-joined-update-hints")));
        assertTrue(response.examples().stream().anyMatch(example -> example.id().equals("sqlserver-merge-output")));
        assertTrue(response.examples().stream().anyMatch(example -> example.id().equals("oracle-offset-fetch")));
        assertTrue(response.examples().stream().anyMatch(example -> example.id().equals("oracle-hierarchical-query")));
    }

    @Test
    void oracleHierarchicalQueryExampleCanBeTranspiled() {
        var example = new ExampleCatalog().examples().stream()
            .filter(item -> item.id().equals("oracle-hierarchical-query"))
            .findFirst()
            .orElseThrow();

        var result = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.ORACLE)
            .targetDialect(SqlDialectId.POSTGRESQL)
            .build()
            .transpile(example.sql());

        assertEquals(TranspileStatus.SUCCESS, result.status());
        assertTrue(result.sql().orElseThrow().contains("WITH RECURSIVE"));
    }
}
