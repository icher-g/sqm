package io.sqm.parser.ansi;

import io.sqm.core.MergeInsertAction;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MergeInsertActionParserTest {

    @Test
    void rejectsMergeInsertByDefault() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(MergeInsertAction.class, "INSERT (id) VALUES (src.id)");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("not supported"));
    }

    @Test
    void parsesSharedMergeInsertActionThroughSupportedHook() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var parser = new SupportedMergeInsertActionParser();

        var withColumns = ctx.parse(parser, "INSERT (id, name) VALUES (src.id, src.name)");
        var withoutColumns = ctx.parse(parser, "INSERT VALUES (src.id)");

        assertTrue(withColumns.ok(), withColumns.errorMessage());
        assertEquals(2, withColumns.value().columns().size());
        assertEquals(2, withColumns.value().values().items().size());

        assertTrue(withoutColumns.ok(), withoutColumns.errorMessage());
        assertTrue(withoutColumns.value().columns().isEmpty());
        assertEquals(1, withoutColumns.value().values().items().size());
    }

    @Test
    void rejectsMalformedSharedMergeInsertAction() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var parser = new SupportedMergeInsertActionParser();

        assertTrue(ctx.parse(parser, "INSERT (id,) VALUES (src.id)").isError());
        assertTrue(ctx.parse(parser, "INSERT (id) VALUES").isError());
    }

    @Test
    void exposesMergeInsertActionTargetType() {
        assertEquals(MergeInsertAction.class, new MergeInsertActionParser().targetType());
    }

    private static final class SupportedMergeInsertActionParser extends MergeInsertActionParser {
        @Override
        public ParseResult<? extends MergeInsertAction> parse(Cursor cur, ParseContext ctx) {
            return parseSupportedAction(cur, ctx);
        }
    }
}
