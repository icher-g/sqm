package io.sqm.parser.ansi;

import io.sqm.core.MergeUpdateAction;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MergeUpdateActionParserTest {

    @Test
    void rejectsMergeUpdateByDefault() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(MergeUpdateAction.class, "UPDATE SET name = src.name");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("not supported"));
    }

    @Test
    void parsesSharedMergeUpdateActionThroughSupportedHook() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var parser = new SupportedMergeUpdateActionParser();
        var result = ctx.parse(parser, "UPDATE SET name = src.name, active = true");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals(2, result.value().assignments().size());
    }

    @Test
    void rejectsMalformedSharedMergeUpdateAction() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var parser = new SupportedMergeUpdateActionParser();

        assertTrue(ctx.parse(parser, "UPDATE name = src.name").isError());
        assertTrue(ctx.parse(parser, "UPDATE SET").isError());
    }

    @Test
    void exposesMergeUpdateActionTargetType() {
        assertEquals(MergeUpdateAction.class, new MergeUpdateActionParser().targetType());
    }

    private static final class SupportedMergeUpdateActionParser extends MergeUpdateActionParser {
        @Override
        public ParseResult<? extends MergeUpdateAction> parse(Cursor cur, ParseContext ctx) {
            return parseSupportedAction(cur, ctx);
        }
    }
}
