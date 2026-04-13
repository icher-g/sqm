package io.sqm.parser.ansi;

import io.sqm.core.DistinctSpec;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DistinctSpecParserTest {

    @Test
    void rejectsDistinctOnAtOnKeywordPosition() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(DistinctSpec.class, "DISTINCT ON (c)");

        assertTrue(result.isError());
        assertEquals(9, result.problems().getFirst().pos());
        assertEquals("DISTINCT ON is not supported by this dialect at 9", result.errorMessage());
    }
}
