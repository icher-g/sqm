package io.sqm.playground.rest;

import io.sqm.playground.api.SqlDialectDto;
import io.sqm.playground.api.RenderParameterizationModeDto;
import io.sqm.playground.api.TranspileOutcomeDto;
import io.sqm.playground.api.TranspileRequestDto;
import io.sqm.playground.rest.service.PlaygroundStatementSupport;
import io.sqm.playground.rest.service.TranspileService;
import org.junit.jupiter.api.Test;

import java.util.List;

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
    void modelDiagnosticsRetainClauseAndDialectMetadataAndNativeOracleIsExact() {
        var service = new TranspileService(new PlaygroundStatementSupport());
        var sql = "SELECT sales FROM data MODEL DIMENSION BY (y) MEASURES (sales) (sales[2026] = sales[2025] + 1)";
        var exact = service.transpile(new TranspileRequestDto(sql, SqlDialectDto.oracle, SqlDialectDto.oracle));
        assertTrue(exact.success(), () -> exact.diagnostics().toString());
        assertEquals(TranspileOutcomeDto.exact, exact.outcome());
        for (var target : List.of(SqlDialectDto.ansi, SqlDialectDto.mysql, SqlDialectDto.postgresql, SqlDialectDto.sqlserver)) {
            var result = service.transpile(new TranspileRequestDto(sql, SqlDialectDto.oracle, target));
            assertFalse(result.success());
            assertNull(result.renderedSql());
            var problem = result.diagnostics().getFirst();
            assertEquals("UNSUPPORTED_MODEL_CLAUSE", problem.code());
            assertEquals("select.model[0]", problem.clausePath());
            assertEquals("oracle", problem.sourceDialect());
            assertEquals(target.name(), problem.targetDialect());
            assertEquals(1, problem.statementIndex());
        }
    }

    @Test
    void transpileReturnsRenderedSqlForExactRewrite() {
        var service = new TranspileService(new PlaygroundStatementSupport());

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
        var service = new TranspileService(new PlaygroundStatementSupport());

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
    void transpileReturnsCombinedSqlForStatementSequence() {
        var service = new TranspileService(new PlaygroundStatementSupport());

        var response = service.transpile(new TranspileRequestDto(
            "select first_name || ' ' || last_name as full_name from users; select id from users;",
            SqlDialectDto.postgresql,
            SqlDialectDto.mysql
        ));

        assertTrue(response.success());
        assertEquals(TranspileOutcomeDto.exact, response.outcome());
        assertNotNull(response.renderedSql());
        assertEquals(2, response.renderedSql().chars().filter(ch -> ch == ';').count());
        assertTrue(response.renderedSql().toLowerCase().contains("concat"));
        assertTrue(response.diagnostics().isEmpty());
    }

    @Test
    void transpileReturnsBindParamsWhenRequested() {
        var service = new TranspileService(new PlaygroundStatementSupport());

        var response = service.transpile(new TranspileRequestDto(
            "select id from users where name = 'alice'",
            SqlDialectDto.postgresql,
            SqlDialectDto.mysql,
            RenderParameterizationModeDto.bind
        ));

        assertTrue(response.success());
        assertEquals(TranspileOutcomeDto.exact, response.outcome());
        assertNotNull(response.renderedSql());
        assertTrue(response.renderedSql().contains("?"));
        assertFalse(response.renderedSql().contains("'alice'"));
        assertEquals(List.of("alice"), response.params());
        assertTrue(response.diagnostics().isEmpty());
    }

    @Test
    void transpileReturnsStatementIndexedDiagnosticsForUnsupportedStatementSequence() {
        var service = new TranspileService(new PlaygroundStatementSupport());

        var response = service.transpile(new TranspileRequestDto(
            "select id from users; select distinct on (user_id) user_id from orders order by user_id;",
            SqlDialectDto.postgresql,
            SqlDialectDto.mysql
        ));

        assertFalse(response.success());
        assertEquals(TranspileOutcomeDto.unsupported, response.outcome());
        assertNull(response.renderedSql());
        assertFalse(response.diagnostics().isEmpty());
        assertEquals("UNSUPPORTED_DISTINCT_ON", response.diagnostics().getFirst().code());
        assertEquals(2, response.diagnostics().getFirst().statementIndex());
    }

    @Test
    void transpileReturnsStatementIndexedDiagnosticsForApproximateStatementSequence() {
        var service = new TranspileService(new PlaygroundStatementSupport());

        var response = service.transpile(new TranspileRequestDto(
            "select * from users use index (idx_users_name); select id from users;",
            SqlDialectDto.mysql,
            SqlDialectDto.postgresql
        ));

        assertTrue(response.success());
        assertEquals(TranspileOutcomeDto.approximate, response.outcome());
        assertFalse(response.diagnostics().isEmpty());
        assertEquals("MYSQL_HINTS_DROPPED", response.diagnostics().getFirst().code());
        assertEquals(1, response.diagnostics().getFirst().statementIndex());
    }

    @Test
    void transpileReturnsDiagnosticsForInvalidSql() {
        var service = new TranspileService(new PlaygroundStatementSupport());

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
        assertEquals(1, response.diagnostics().getFirst().line());
        assertEquals(8, response.diagnostics().getFirst().column());
        assertEquals("ansi", response.diagnostics().getFirst().sourceDialect());
        assertEquals("mysql", response.diagnostics().getFirst().targetDialect());
    }

    @Test
    void transpileReturnsErrorDiagnosticWhenRequestCannotBeMapped() {
        var service = new TranspileService(new PlaygroundStatementSupport());

        var response = service.transpile(new TranspileRequestDto(
            "select 1",
            null,
            SqlDialectDto.mysql
        ));

        assertFalse(response.success());
        assertEquals(TranspileOutcomeDto.unsupported, response.outcome());
        assertNull(response.renderedSql());
        assertTrue(response.params().isEmpty());
        assertEquals("TRANSPILE_ERROR", response.diagnostics().getFirst().code());
    }
}
