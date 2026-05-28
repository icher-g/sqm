package io.sqm.parser.ansi;

import io.sqm.core.SampledTable;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class SampledTableParserTest {
    @Test
    void standaloneParseReportsMissingSource() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var parser = new SampledTableParser();

        var result = parser.parse(Cursor.of("TABLESAMPLE SYSTEM (10)", ctx.identifierQuoting()), ctx);

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("requires a source table"));
        assertEquals(SampledTable.class, parser.targetType());
    }

    @Test
    void matchesOnlyTableSampleKeyword() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var parser = new SampledTableParser();

        assertTrue(parser.match(Cursor.of("TABLESAMPLE SYSTEM (10)", ctx.identifierQuoting()), ctx));
        assertFalse(parser.match(Cursor.of("SAMPLE (10)", ctx.identifierQuoting()), ctx));
    }
}
