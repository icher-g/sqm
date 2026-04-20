package io.sqm.playground.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests shared playground API contract DTOs.
 */
class PlaygroundApiContractsTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldSerializeDialectUsingLowercaseWireValue() throws Exception {
        String json = mapper.writeValueAsString(SqlDialectDto.postgresql);

        assertEquals("\"postgresql\"", json);
    }

    @Test
    void shouldDeserializeDialectFromLowercaseWireValue() throws Exception {
        SqlDialectDto dialect = mapper.readValue("\"mysql\"", SqlDialectDto.class);

        assertEquals(SqlDialectDto.mysql, dialect);
    }

    @Test
    void shouldRejectUnknownDialectValue() {
        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> SqlDialectDto.fromValue("oracle")
        );

        assertTrue(error.getMessage().contains("Unknown SQL dialect"));
    }

    @Test
    void shouldSerializeDiagnosticWithExpectedWireShape() throws Exception {
        PlaygroundDiagnosticDto diagnostic = new PlaygroundDiagnosticDto(
            DiagnosticSeverityDto.error,
            "PARSER_UNEXPECTED_TOKEN",
            "Expected FROM but found WHERE",
            DiagnosticPhaseDto.parse,
            1,
            15
        );

        String json = mapper.writeValueAsString(diagnostic);

        assertTrue(json.contains("\"severity\":\"error\""));
        assertTrue(json.contains("\"code\":\"PARSER_UNEXPECTED_TOKEN\""));
        assertTrue(json.contains("\"phase\":\"parse\""));
        assertTrue(json.contains("\"line\":1"));
        assertTrue(json.contains("\"column\":15"));
    }

    @Test
    void shouldExposeCommonResponseFieldsThroughResponseContract() throws Exception {
        SampleResponseDto response = new SampleResponseDto(
            "req-123",
            true,
            12L,
            List.of(new PlaygroundDiagnosticDto(
                DiagnosticSeverityDto.info,
                "EXAMPLE",
                "Example diagnostic",
                DiagnosticPhaseDto.http,
                null,
                null
            ))
        );

        String json = mapper.writeValueAsString(response);

        assertTrue(json.contains("\"requestId\":\"req-123\""));
        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("\"durationMs\":12"));
        assertTrue(json.contains("\"diagnostics\""));
    }

    @Test
    void shouldSerializeParseResponseWithAstAndSummaryShape() throws Exception {
        ParseResponseDto response = new ParseResponseDto(
            "req-parse",
            true,
            12L,
            "query",
            false,
            "{ \"kind\": \"select\" }",
            "public static SelectQuery getStatement() { return select().build(); }",
            new AstNodeDto(
                "SelectQuery",
                "io.sqm.core.Query",
                "select",
                "statement",
                "SelectQuery",
                List.of(new AstDetailDto("kind", "select")),
                List.of(new AstChildSlotDto("items", true, List.of()))
            ),
            new ParseResponseSummaryDto("SelectQuery", "io.sqm.core.Query"),
            List.of()
        );

        String json = mapper.writeValueAsString(response);

        assertTrue(json.contains("\"statementKind\":\"query\""));
        assertTrue(json.contains("\"multiStatement\":false"));
        assertTrue(json.contains("\"sqmJson\":\"{ \\\"kind\\\": \\\"select\\\" }\""));
        assertTrue(json.contains("\"sqmDsl\":\"public static SelectQuery getStatement() { return select().build(); }\""));
        assertTrue(json.contains("\"ast\""));
        assertTrue(json.contains("\"nodeType\":\"SelectQuery\""));
        assertTrue(json.contains("\"nodeInterface\":\"io.sqm.core.Query\""));
        assertTrue(json.contains("\"kind\":\"select\""));
        assertTrue(json.contains("\"category\":\"statement\""));
        assertTrue(json.contains("\"slot\":\"items\""));
        assertTrue(json.contains("\"rootNodeType\":\"SelectQuery\""));
        assertTrue(json.contains("\"rootInterface\":\"io.sqm.core.Query\""));
    }

    @Test
    void shouldSerializeExamplesResponseWithDialectValue() throws Exception {
        ExamplesResponseDto response = new ExamplesResponseDto(
            "req-examples",
            true,
            2L,
            List.of(new ExampleDto("basic-select", "Basic SELECT", SqlDialectDto.ansi, "select 1")),
            List.of()
        );

        String json = mapper.writeValueAsString(response);

        assertTrue(json.contains("\"examples\""));
        assertTrue(json.contains("\"id\":\"basic-select\""));
        assertTrue(json.contains("\"dialect\":\"ansi\""));
    }

    @Test
    void shouldSerializeRenderValidateAndTranspileResponsesUsingExplicitContracts() throws Exception {
        RenderResponseDto render = new RenderResponseDto("req-render", true, 15L, "SELECT 1", List.of());
        ValidateResponseDto validate = new ValidateResponseDto("req-validate", true, 10L, true, List.of());
        TranspileResponseDto transpile = new TranspileResponseDto(
            "req-transpile",
            true,
            18L,
            TranspileOutcomeDto.exact,
            "SELECT TOP 5 * FROM customer",
            List.of()
        );

        String renderJson = mapper.writeValueAsString(render);
        String validateJson = mapper.writeValueAsString(validate);
        String transpileJson = mapper.writeValueAsString(transpile);

        assertTrue(renderJson.contains("\"renderedSql\":\"SELECT 1\""));
        assertTrue(validateJson.contains("\"valid\":true"));
        assertTrue(transpileJson.contains("\"outcome\":\"exact\""));
    }

    @Test
    void shouldDeserializeTranspileOutcomeFromLowercaseWireValue() throws Exception {
        TranspileOutcomeDto outcome = mapper.readValue("\"approximate\"", TranspileOutcomeDto.class);

        assertEquals(TranspileOutcomeDto.approximate, outcome);
    }

    @Test
    void shouldSerializeHealthResponseUsingCommonResponseFields() throws Exception {
        HealthResponseDto response = new HealthResponseDto(
            "req-health",
            true,
            1L,
            "UP",
            List.of()
        );

        String json = mapper.writeValueAsString(response);

        assertTrue(json.contains("\"requestId\":\"req-health\""));
        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("\"durationMs\":1"));
        assertTrue(json.contains("\"status\":\"UP\""));
    }

    /**
     * Test-only response record implementing the shared response contract.
     *
     * @param requestId correlation identifier
     * @param success operation success flag
     * @param durationMs operation duration in milliseconds
     * @param diagnostics structured diagnostics
     */
    private record SampleResponseDto(
        String requestId,
        boolean success,
        long durationMs,
        List<PlaygroundDiagnosticDto> diagnostics
    ) implements PlaygroundResponseDto {
    }
}
