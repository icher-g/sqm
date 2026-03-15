package io.sqm.parser.ansi;

import io.sqm.core.SelectQuery;
import io.sqm.core.SelectQueryBuilder;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectQueryParserTest {

    @Test
    void parsesMinimalSelectUsingSharedHooks() {
        var parser = new SelectQueryParser();
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = parser.parse(Cursor.of("SELECT 1", ctx.identifierQuoting()), ctx);

        assertTrue(result.ok());
        assertEquals(1, result.value().items().size());
        assertNull(result.value().from());
        assertNull(result.value().limitOffset());
    }

    @Test
    void targetTypeIsSelectQuery() {
        assertEquals(SelectQuery.class, new SelectQueryParser().targetType());
    }

    @Test
    void returnsErrorWhenBeforeSelectHookFails() {
        var parser = new HookedParser(ParseResult.error("hook failure", 0));
        var ctx = ParseContext.of(new AnsiSpecs());

        var result = parser.parse(Cursor.of("SELECT 1", ctx.identifierQuoting()), ctx);

        assertFalse(result.ok());
        assertEquals("hook failure at 0", result.errorMessage());
    }

    @Test
    void returnsErrorWhenCompletedValidationFails() {
        var parser = new HookedParser(ParseResult.ok(null), ParseResult.error("done failure", 0));
        var ctx = ParseContext.of(new AnsiSpecs());

        var result = parser.parse(Cursor.of("SELECT 1", ctx.identifierQuoting()), ctx);

        assertFalse(result.ok());
        assertEquals("done failure at 0", result.errorMessage());
    }

    private static final class HookedParser extends SelectQueryParser {
        private final ParseResult<Void> beforeSelect;
        private final ParseResult<Void> completedValidation;

        private HookedParser(ParseResult<Void> beforeSelect) {
            this(beforeSelect, ParseResult.ok(null));
        }

        private HookedParser(ParseResult<Void> beforeSelect, ParseResult<Void> completedValidation) {
            this.beforeSelect = beforeSelect;
            this.completedValidation = completedValidation;
        }

        @Override
        protected ParseResult<Void> parseBeforeSelectKeyword(Cursor cur, ParseContext ctx, SelectQueryBuilder q) {
            return beforeSelect;
        }

        @Override
        protected ParseResult<Void> validateCompletedQuery(Cursor cur, ParseContext ctx, SelectQueryBuilder q) {
            return completedValidation;
        }
    }
}
