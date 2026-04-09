package io.sqm.playground.rest;

import io.sqm.playground.api.SqlDialectDto;
import io.sqm.playground.api.TranspileOutcomeDto;
import io.sqm.playground.api.TranspileRequestDto;
import io.sqm.playground.rest.service.TranspileService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests playground transpile service behavior.
 */
class TranspileServiceTest {

    @Test
    void transpileReturnsRenderedSqlForExactRewrite() {
        var service = new TranspileService();

        var response = service.transpile(new TranspileRequestDto(
            "select first_name || ' ' || last_name as full_name from users",
            SqlDialectDto.postgresql,
            SqlDialectDto.mysql
        ));

        assertTrue(response.success());
        assertEquals(TranspileOutcomeDto.exact, response.outcome());
        assertNotNull(response.renderedSql());
        assertTrue(response.renderedSql().toLowerCase().contains("concat"));
        assertTrue(response.diagnostics().isEmpty());
    }

    @Test
    void transpileReturnsWarningsForApproximateRewrite() {
        var service = new TranspileService();

        var response = service.transpile(new TranspileRequestDto(
            "select * from users where name ilike 'al%'",
            SqlDialectDto.postgresql,
            SqlDialectDto.mysql
        ));

        assertTrue(response.success());
        assertEquals(TranspileOutcomeDto.approximate, response.outcome());
        assertNotNull(response.renderedSql());
        assertFalse(response.diagnostics().isEmpty());
        assertEquals("warning", response.diagnostics().getFirst().severity().name());
    }

    @Test
    void transpileReturnsDiagnosticsForInvalidSql() {
        var service = new TranspileService();

        var response = service.transpile(new TranspileRequestDto(
            "select from",
            SqlDialectDto.ansi,
            SqlDialectDto.mysql
        ));

        assertFalse(response.success());
        assertEquals(TranspileOutcomeDto.unsupported, response.outcome());
        assertNull(response.renderedSql());
        assertFalse(response.diagnostics().isEmpty());
        assertEquals("PARSE_ERROR", response.diagnostics().getFirst().code());
    }
}
