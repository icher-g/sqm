package io.sqm.playground.rest.service;

import io.sqm.core.Assignment;
import io.sqm.core.ColumnExpr;
import io.sqm.core.DeleteStatement;
import io.sqm.core.Expression;
import io.sqm.core.Identifier;
import io.sqm.core.InsertStatement;
import io.sqm.core.MergeStatement;
import io.sqm.core.Query;
import io.sqm.core.TableRef;
import io.sqm.core.UpdateStatement;
import io.sqm.playground.api.DiagnosticPhaseDto;
import io.sqm.playground.api.SqlDialectDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests shared statement parsing and classification helpers.
 */
class PlaygroundStatementSupportTest {

    @Test
    void parsesDialectSpecificSqlAcrossSupportedDialects() {
        var support = new PlaygroundStatementSupport();

        assertTrue(support.parse("select id from customer", SqlDialectDto.ansi).success());
        assertTrue(support.parse("select distinct on (id) id from customer order by id", SqlDialectDto.postgresql).success());
        assertTrue(support.parse("select `id` from `customer`", SqlDialectDto.mysql).success());
        assertTrue(support.parse("select top (1) id from customer", SqlDialectDto.sqlserver).success());
    }

    @Test
    void parsesStatementSequenceAndIgnoresEmptyStatements() {
        var support = new PlaygroundStatementSupport();

        var attempt = support.parseSequence("select 1;; select 2;", SqlDialectDto.ansi);

        assertTrue(attempt.success());
        assertEquals(2, attempt.sequence().statements().size());
    }

    @Test
    void statementKindClassifiesSupportedStatementFamilies() {
        var support = new PlaygroundStatementSupport();

        var query = Query.select(Expression.literal(1)).build();
        var insert = InsertStatement.builder(TableRef.table(Identifier.of("customer")))
            .values(Expression.row(1))
            .build();
        var update = UpdateStatement.builder(TableRef.table(Identifier.of("customer")))
            .set(Identifier.of("name"), Expression.literal("alice"))
            .build();
        var delete = DeleteStatement.builder(TableRef.table(Identifier.of("customer"))).build();
        var merge = MergeStatement.builder(TableRef.table(Identifier.of("customer")))
            .source(TableRef.table(Identifier.of("customer_stage")).as("src"))
            .on(ColumnExpr.of(Identifier.of("customer"), Identifier.of("id"))
                .eq(ColumnExpr.of(Identifier.of("src"), Identifier.of("id"))))
            .whenMatchedUpdate(Assignment.of(Identifier.of("name"), ColumnExpr.of(Identifier.of("src"), Identifier.of("name"))))
            .build();

        assertEquals("query", support.statementKind(query));
        assertEquals("insert", support.statementKind(insert));
        assertEquals("update", support.statementKind(update));
        assertEquals("delete", support.statementKind(delete));
        assertEquals("merge", support.statementKind(merge));
    }

    @Test
    void failedParseReturnsStructuredDiagnostics() {
        var support = new PlaygroundStatementSupport();

        var attempt = support.parse("select from", SqlDialectDto.ansi);

        assertFalse(attempt.success());
        assertEquals("PARSE_ERROR", attempt.diagnostics().getFirst().code());
        assertEquals(1, attempt.diagnostics().getFirst().line());
        assertEquals(8, attempt.diagnostics().getFirst().column());
    }

    @Test
    void failedParseReportsLineAndColumnForMultilineSql() {
        var support = new PlaygroundStatementSupport();

        var attempt = support.parse("select id\nwrite form customer", SqlDialectDto.ansi);

        assertFalse(attempt.success());
        assertEquals("PARSE_ERROR", attempt.diagnostics().getFirst().code());
        assertEquals(2, attempt.diagnostics().getFirst().line());
        assertEquals(7, attempt.diagnostics().getFirst().column());
    }

    @Test
    void createsStructuredDiagnosticsForOperationFailures() {
        var support = new PlaygroundStatementSupport();
        var diagnostic = support.diagnostic(DiagnosticPhaseDto.render, "RENDER_ERROR", "Cannot render query");

        assertEquals("RENDER_ERROR", diagnostic.code());
        assertEquals("render", diagnostic.phase().name());
        assertEquals("error", diagnostic.severity().name());
    }
}
