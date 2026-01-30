package io.sqm.parser.ansi;

import io.sqm.core.LimitOffset;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class LimitOffsetParserTest {

    private final ParseContext ctx = ParseContext.of(new AnsiSpecs());
    private final LimitOffsetParser parser = new LimitOffsetParser();

    @Test
    void parsesLimitOnly() {
        var result = ctx.parse(parser, "LIMIT 10");

        assertTrue(result.ok());
        assertEquals(LimitOffset.of(10L, null), result.value());
    }

    @Test
    void parsesOffsetWithRowsAndFetch() {
        var result = ctx.parse(parser, "OFFSET 5 ROW FETCH NEXT 3 ROWS ONLY");

        assertTrue(result.ok());
        assertEquals(LimitOffset.of(3L, 5L), result.value());
    }

    @Test
    void parsesFetchOnly() {
        var result = ctx.parse(parser, "FETCH FIRST 2 ROWS ONLY");

        assertTrue(result.ok());
        assertEquals(LimitOffset.of(2L, null), result.value());
    }

    @Test
    void errorsOnLimitWithoutNumber() {
        var result = ctx.parse(parser, "LIMIT x");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Expected number after LIMIT"));
    }

    @Test
    void errorsOnFetchWithoutFirstOrNext() {
        var result = ctx.parse(parser, "FETCH ONLY");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Expected FIRST or NEXT after FETCH"));
    }

    @Test
    void errorsOnFetchMissingOnly() {
        var result = ctx.parse(parser, "FETCH FIRST 1 ROWS");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Expected ONLY"));
    }
}
