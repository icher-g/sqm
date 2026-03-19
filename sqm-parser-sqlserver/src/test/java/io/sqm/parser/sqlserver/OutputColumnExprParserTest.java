package io.sqm.parser.sqlserver;

import io.sqm.core.OutputColumnExpr;
import io.sqm.core.OutputRowSource;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.sqlserver.spi.SqlServerSpecs;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutputColumnExprParserTest {

    @Test
    void parsesInsertedOutputColumnReference() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(OutputColumnExpr.class, "inserted.id");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals(OutputRowSource.INSERTED, result.value().source());
        assertEquals("id", result.value().column().value());
    }

    @Test
    void parsesDeletedOutputColumnReference() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(OutputColumnExpr.class, "deleted.id");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals(OutputRowSource.DELETED, result.value().source());
        assertEquals("id", result.value().column().value());
    }

    @Test
    void rejectsUnknownPseudoRowSource() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(OutputColumnExpr.class, "unknown.id");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Expected inserted.<column> or deleted.<column>"));
    }

    @Test
    void rejectsMissingDotBetweenSourceAndColumn() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(OutputColumnExpr.class, "inserted id");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Expected inserted.<column> or deleted.<column>"));
    }
}
