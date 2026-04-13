package io.sqm.parser.spi;

import io.sqm.core.ColumnExpr;
import io.sqm.core.Statement;
import io.sqm.parser.core.ParserException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.col;
import static org.junit.jupiter.api.Assertions.*;

class ParseResultTest {

    @Test
    void shouldCreateSuccessResultWithOk() {
        var column = col("test");
        var result = ParseResult.ok(column);

        assertSame(column, result.value());
        assertTrue(result.problems().isEmpty());
        assertTrue(result.ok());
        assertFalse(result.isError());
    }

    @Test
    void shouldCreateErrorResultWithMessage() {
        var result = ParseResult.<ColumnExpr>error("test error", 10);

        assertNull(result.value());
        assertEquals(1, result.problems().size());
        assertEquals("test error", result.problems().getFirst().message());
        assertEquals(10, result.problems().getFirst().pos());
        assertNull(result.problems().getFirst().line());
        assertNull(result.problems().getFirst().column());
        assertFalse(result.ok());
        assertTrue(result.isError());
    }

    @Test
    void shouldCreateErrorResultFromException() {
        var exception = new ParserException("parser error", 5, 2, 4);
        var result = ParseResult.<ColumnExpr>error(exception);

        assertNull(result.value());
        assertEquals(1, result.problems().size());
        assertEquals("parser error", result.problems().getFirst().message());
        assertEquals(5, result.problems().getFirst().pos());
        assertEquals(2, result.problems().getFirst().line());
        assertEquals(4, result.problems().getFirst().column());
        assertFalse(result.ok());
        assertTrue(result.isError());
    }

    @Test
    void shouldCreateErrorResultFromAnotherResult() {
        var sourceResult = ParseResult.<ColumnExpr>error("source error", 15);
        var result = ParseResult.<String>error(sourceResult);

        assertNull(result.value());
        assertEquals(1, result.problems().size());
        assertEquals("source error", result.problems().getFirst().message());
        assertEquals(15, result.problems().getFirst().pos());
    }

    @Test
    void shouldReturnErrorMessageForSingleProblem() {
        var result = ParseResult.<ColumnExpr>error("test error", 10);

        assertEquals("test error at 10", result.errorMessage());
    }

    @Test
    void shouldReturnCombinedErrorMessageForMultipleProblems() {
        var problems = List.of(
            new ParseProblem("error 1", 1),
            new ParseProblem("error 2", 2)
        );
        var result = new ParseResult<ColumnExpr>(null, problems);

        String errorMessage = result.errorMessage();
        assertEquals("error 1 at 1", errorMessage); // Returns first error
    }

    @Test
    void shouldReturnNullErrorMessageForSuccess() {
        var column = col("test");
        var result = ParseResult.ok(column);

        assertNull(result.errorMessage());
    }

    @Test
    void parseContextAttachesLineAndColumnToProblems() {
        ParseResult<Statement> result;
        try (var ignored = ParseLocations.open("select from")) {
            result = ParseResult.error("Expected FROM", 7);
        }

        assertTrue(result.isError());
        assertEquals(1, result.problems().getFirst().line());
        assertEquals(8, result.problems().getFirst().column());
    }

    @Test
    void parseContextAttachesLineAndColumnToLexerProblems() {
        ParseResult<Statement> result;
        try (var ignored = ParseLocations.open("select 'unterminated")) {
            result = ParseResult.error(new ParserException("Unterminated string literal", 7));
        }

        assertTrue(result.isError());
        assertEquals(1, result.problems().getFirst().line());
        assertEquals(8, result.problems().getFirst().column());
    }
}

