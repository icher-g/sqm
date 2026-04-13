package io.sqm.parser.ansi;

import io.sqm.core.Predicate;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LikePredicateParserTest {

    @Test
    void rejectsIlikeAtIlikeKeywordPosition() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(Predicate.class, "c ILIKE 'x'");

        assertTrue(result.isError());
        assertEquals(2, result.problems().getFirst().pos());
        assertEquals("ILIKE is not supported by this dialect at 2", result.errorMessage());
    }
}
