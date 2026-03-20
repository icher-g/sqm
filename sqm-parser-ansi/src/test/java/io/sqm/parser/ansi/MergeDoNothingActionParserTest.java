package io.sqm.parser.ansi;

import io.sqm.core.MergeDoNothingAction;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MergeDoNothingActionParserTest {

    @Test
    void rejectsDoNothingByDefault() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(MergeDoNothingAction.class, "DO NOTHING");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("not supported"));
    }

    @Test
    void rejectsMalformedDoNothingAction() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(MergeDoNothingAction.class, "DO UPDATE");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("NOTHING"));
    }

    @Test
    void exposesMergeDoNothingActionTargetType() {
        assertEquals(MergeDoNothingAction.class, new MergeDoNothingActionParser().targetType());
    }
}
