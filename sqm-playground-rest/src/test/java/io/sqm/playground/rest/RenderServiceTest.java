package io.sqm.playground.rest;

import io.sqm.playground.api.RenderRequestDto;
import io.sqm.playground.api.SqlDialectDto;
import io.sqm.playground.rest.service.PlaygroundStatementSupport;
import io.sqm.playground.rest.service.RenderService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests playground render service behavior.
 */
class RenderServiceTest {

    @Test
    void renderReturnsRenderedSqlForValidSql() {
        var service = new RenderService(new PlaygroundStatementSupport());

        var response = service.render(new RenderRequestDto(
            "select id, name from customer",
            SqlDialectDto.ansi,
            SqlDialectDto.postgresql
        ));

        assertTrue(response.success());
        assertNotNull(response.renderedSql());
        assertTrue(response.renderedSql().toLowerCase().contains("select"));
        assertTrue(response.diagnostics().isEmpty());
    }

    @Test
    void renderReturnsParseDiagnosticsForInvalidSql() {
        var service = new RenderService(new PlaygroundStatementSupport());

        var response = service.render(new RenderRequestDto(
            "select from",
            SqlDialectDto.ansi,
            SqlDialectDto.mysql
        ));

        assertFalse(response.success());
        assertNull(response.renderedSql());
        assertFalse(response.diagnostics().isEmpty());
        assertEquals("PARSE_ERROR", response.diagnostics().getFirst().code());
    }
}
