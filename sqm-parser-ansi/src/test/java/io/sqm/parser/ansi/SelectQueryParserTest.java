package io.sqm.parser.ansi;

import io.sqm.core.SelectQuery;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}