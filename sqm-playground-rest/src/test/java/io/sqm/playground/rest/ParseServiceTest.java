package io.sqm.playground.rest;

import io.sqm.playground.api.ParseRequestDto;
import io.sqm.playground.api.SqlDialectDto;
import io.sqm.playground.rest.service.ParseService;
import io.sqm.playground.rest.service.PlaygroundStatementSupport;
import io.sqm.playground.api.AstChildSlotDto;
import io.sqm.playground.api.AstNodeDto;
import io.sqm.playground.rest.service.SqmAstMapper;
import io.sqm.playground.rest.service.SqmDslGenerator;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests playground parse service behavior.
 */
class ParseServiceTest {

    @Test
    void parseReturnsSqmJsonAndAstForValidSql() {
        var service = new ParseService(new SqmAstMapper(), new SqmDslGenerator(), new PlaygroundStatementSupport());

        var response = service.parse(new ParseRequestDto(
            "select c.id, c.name from customer c where c.id = 1 order by c.name",
            SqlDialectDto.ansi
        ));

        assertTrue(response.success());
        assertEquals("query", response.statementKind());
        assertNotNull(response.sqmJson());
        assertTrue(response.sqmJson().contains("\"kind\""));
        assertNotNull(response.sqmDsl());
        assertTrue(response.sqmDsl().contains("public static SelectQuery getStatement()"));
        assertTrue(response.sqmDsl().contains("return builder.select("));
        assertNotNull(response.summary());
        assertEquals("SelectQuery", response.summary().rootInterface().substring(response.summary().rootInterface().lastIndexOf('.') + 1));
        assertNotNull(response.ast());
        assertEquals("SelectQuery", response.ast().nodeType());
        assertEquals("io.sqm.core.SelectQuery", response.ast().nodeInterface());
        assertEquals("statement", response.ast().category());

        var items = slot(response.ast(), "items");
        assertTrue(items.multiple());
        assertEquals(2, items.nodes().size());
        assertEquals("ExprSelectItem", items.nodes().getFirst().nodeType());

        var from = slot(response.ast(), "from");
        assertFalse(from.multiple());
        assertEquals("Table", from.nodes().getFirst().nodeType());

        var where = slot(response.ast(), "where");
        assertEquals("ComparisonPredicate", where.nodes().getFirst().nodeType());

        var orderBy = slot(response.ast(), "orderBy");
        assertEquals("OrderBy", orderBy.nodes().getFirst().nodeType());
        assertTrue(response.diagnostics().isEmpty());
    }

    @Test
    void parseReturnsDiagnosticsForInvalidSql() {
        var service = new ParseService(new SqmAstMapper(), new SqmDslGenerator(), new PlaygroundStatementSupport());

        var response = service.parse(new ParseRequestDto(
            "select from",
            SqlDialectDto.ansi
        ));

        assertFalse(response.success());
        assertNull(response.sqmJson());
        assertNull(response.sqmDsl());
        assertNull(response.summary());
        assertFalse(response.diagnostics().isEmpty());
        assertEquals("PARSE_ERROR", response.diagnostics().getFirst().code());
    }

    private static AstChildSlotDto slot(AstNodeDto node, String slot) {
        return node.children().stream()
            .filter(child -> Objects.equals(slot, child.slot()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing AST slot: " + slot));
    }
}
