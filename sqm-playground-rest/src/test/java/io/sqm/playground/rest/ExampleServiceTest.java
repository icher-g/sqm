package io.sqm.playground.rest;

import io.sqm.core.dialect.SqlDialectId;
import io.sqm.playground.api.DiagnosticPhaseDto;
import io.sqm.playground.api.DiagnosticSeverityDto;
import io.sqm.playground.api.ParseRequestDto;
import io.sqm.playground.api.RenderRequestDto;
import io.sqm.playground.api.SqlDialectDto;
import io.sqm.playground.rest.example.ExampleCatalog;
import io.sqm.playground.rest.service.ExampleService;
import io.sqm.playground.rest.service.ParseService;
import io.sqm.playground.rest.service.PlaygroundStatementSupport;
import io.sqm.playground.rest.service.RenderService;
import io.sqm.playground.rest.service.SqmAstMapper;
import io.sqm.playground.rest.service.SqmDslGenerator;
import io.sqm.transpile.SqlTranspiler;
import io.sqm.transpile.TranspileStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        assertEquals(13, response.examples().size());
        assertEquals("basic-select", response.examples().getFirst().id());
        assertEquals("ansi", response.examples().getFirst().dialect().name());
        assertTrue(response.examples().stream().anyMatch(example -> example.id().equals("ansi-analytics-report")));
        assertTrue(response.examples().stream().anyMatch(example -> example.id().equals("postgres-merge-returning")));
        assertTrue(response.examples().stream().anyMatch(example -> example.id().equals("mysql-joined-update-hints")));
        assertTrue(response.examples().stream().anyMatch(example -> example.id().equals("sqlserver-merge-output")));
        assertTrue(response.examples().stream().anyMatch(example -> example.id().equals("oracle-offset-fetch")));
        assertTrue(response.examples().stream().anyMatch(example -> example.id().equals("oracle-hierarchical-query")));
        assertTrue(response.examples().stream().anyMatch(example -> example.id().equals("oracle-pivot-query")));
        assertTrue(response.examples().stream().anyMatch(example -> example.id().equals("oracle-match-recognize")));
        assertTrue(response.examples().stream().anyMatch(example -> example.id().equals("oracle-model")));
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

    @Test
    void oracleMatchRecognizeExampleParsesValidatesAndRenders() {
        var example = new ExampleCatalog().examples().stream()
            .filter(item -> item.id().equals("oracle-match-recognize"))
            .findFirst()
            .orElseThrow();

        var result = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.ORACLE)
            .targetDialect(SqlDialectId.ORACLE)
            .build()
            .transpile(example.sql());

        assertEquals(TranspileStatus.SUCCESS, result.status());
        assertTrue(result.sql().orElseThrow().contains("MATCH_RECOGNIZE"));
    }

    @Test
    void oracleModelExampleCanBeInspectedAndFormattedWhileCodegenIsPending() {
        var example = new ExampleCatalog().examples().stream()
            .filter(item -> item.id().equals("oracle-model"))
            .findFirst()
            .orElseThrow();
        assertEquals(SqlDialectDto.oracle, example.dialect());
        var support = new PlaygroundStatementSupport();
        var parser = new ParseService(new SqmAstMapper(), new SqmDslGenerator(), support);

        var parsed = parser.parse(new ParseRequestDto(example.sql(), example.dialect()));

        assertTrue(parsed.success(), () -> parsed.diagnostics().toString());
        assertTrue(parsed.sqmJson().contains("model-clause"));
        var model = parsed.ast().children().stream()
            .filter(slot -> slot.slot().equals("model"))
            .findFirst().orElseThrow().nodes().getFirst();
        assertEquals("ModelClause", model.nodeType());
        assertNull(parsed.sqmDsl());
        assertEquals(1, parsed.diagnostics().size());
        var diagnostic = parsed.diagnostics().getFirst();
        assertEquals(DiagnosticPhaseDto.dsl, diagnostic.phase());
        assertEquals(DiagnosticSeverityDto.warning, diagnostic.severity());
        assertEquals("DSL_GENERATION_ERROR", diagnostic.code());
        assertTrue(diagnostic.message().contains("MODEL code generation"));

        var rendered = new RenderService(support).render(new RenderRequestDto(
            example.sql(), example.dialect(), example.dialect()));

        assertTrue(rendered.success(), () -> rendered.diagnostics().toString());
        assertTrue(rendered.renderedSql().contains("MODEL"));
        assertTrue(rendered.renderedSql().contains("sales['Bike', 2026] = sales['Bike', 2025] * 1.1"));
        assertTrue(rendered.renderedSql().contains("\nMODEL\n  PARTITION BY (country)"));
        assertTrue(rendered.renderedSql().contains("\n  RULES (\n    sales['Bike', 2026]"));
        assertTrue(rendered.renderedSql().contains(",\n    sales['Car', 2026]"));
        assertTrue(rendered.renderedSql().contains("\n  )\nORDER BY country, product, sales_year"));
        var reparsed = parser.parse(new ParseRequestDto(rendered.renderedSql(), example.dialect()));
        assertTrue(reparsed.success());
        assertEquals(parsed.sqmJson(), reparsed.sqmJson());
    }
}
