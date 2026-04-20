package io.sqm.playground.rest;

import io.sqm.playground.api.RenderRequestDto;
import io.sqm.playground.api.RenderParameterizationModeDto;
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
        assertTrue(response.params().isEmpty());
        assertTrue(response.diagnostics().isEmpty());
    }

    @Test
    void renderReturnsCombinedSqlForStatementSequence() {
        var service = new RenderService(new PlaygroundStatementSupport());

        var response = service.render(new RenderRequestDto(
            "select 1; select 2;",
            SqlDialectDto.ansi,
            SqlDialectDto.postgresql
        ));

        assertTrue(response.success());
        assertNotNull(response.renderedSql());
        assertTrue(response.renderedSql().stripTrailing().endsWith(";"));
        assertEquals(2, response.renderedSql().chars().filter(ch -> ch == ';').count());
        assertTrue(response.renderedSql().toLowerCase().contains("select 1"));
        assertTrue(response.renderedSql().toLowerCase().contains("select 2"));
        assertTrue(response.params().isEmpty());
        assertTrue(response.diagnostics().isEmpty());
    }

    @Test
    void renderPreservesMultilineFormattingForUpdateClauses() {
        var service = new RenderService(new PlaygroundStatementSupport());

        var response = service.render(new RenderRequestDto(
            "update orders o join customer c on c.id = o.customer_id set o.status = 'priority' where c.vip = 1",
            SqlDialectDto.mysql,
            SqlDialectDto.mysql
        ));

        assertTrue(response.success());
        assertNotNull(response.renderedSql());
        assertTrue(response.renderedSql().contains("\nINNER JOIN customer AS c ON c.id = o.customer_id"));
        assertTrue(response.renderedSql().contains("\nSET o.status = 'priority'"));
        assertTrue(response.renderedSql().contains("\nWHERE c.vip = 1"));
    }

    @Test
    void renderReturnsBindParametersWhenParameterizationIsEnabled() {
        var service = new RenderService(new PlaygroundStatementSupport());

        var response = service.render(new RenderRequestDto(
            "select id from customer where id = 7; select name from customer where name = 'alice';",
            SqlDialectDto.ansi,
            SqlDialectDto.postgresql,
            RenderParameterizationModeDto.bind
        ));

        assertTrue(response.success());
        assertNotNull(response.renderedSql());
        assertTrue(response.renderedSql().contains("?"));
        assertEquals(2, response.params().size());
        assertTrue(response.params().contains(7L));
        assertTrue(response.params().contains("alice"));
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
        assertTrue(response.params().isEmpty());
        assertFalse(response.diagnostics().isEmpty());
        assertEquals("PARSE_ERROR", response.diagnostics().getFirst().code());
    }
}
