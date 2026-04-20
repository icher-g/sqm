package io.sqm.parser.ansi;

import io.sqm.core.InsertStatement;
import io.sqm.core.Query;
import io.sqm.core.StatementSequence;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatementSequenceParserTest {

    @Test
    void ansiContextParsesMultipleStatements() {
        var ctx = ParseContext.of(new AnsiSpecs());

        var result = ctx.parse(StatementSequence.class, "SELECT 1; INSERT INTO users (id) VALUES (1);");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals(2, result.value().statements().size());
        assertInstanceOf(Query.class, result.value().statements().get(0));
        assertInstanceOf(InsertStatement.class, result.value().statements().get(1));
    }

    @Test
    void ansiContextIgnoresEmptyStatements() {
        var ctx = ParseContext.of(new AnsiSpecs());

        var result = ctx.parse(StatementSequence.class, ";; SELECT ';';;");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals(1, result.value().statements().size());
    }
}
