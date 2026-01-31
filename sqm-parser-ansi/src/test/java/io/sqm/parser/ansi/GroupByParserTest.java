package io.sqm.parser.ansi;

import io.sqm.core.GroupBy;
import io.sqm.core.GroupItem;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ANSI GroupByParser Tests")
class GroupByParserTest {

    private final ParseContext ctx = ParseContext.of(new AnsiSpecs());

    @Test
    @DisplayName("Parse GROUP BY list")
    void parsesGroupByList() {
        var result = ctx.parse(GroupBy.class, "GROUP BY a, b");

        assertTrue(result.ok(), () -> "expected ok, got error: " + result.errorMessage());
        assertEquals(2, result.value().items().size());
        var first = (GroupItem.SimpleGroupItem) result.value().items().getFirst();
        assertEquals("a", first.expr().matchExpression().column(c -> c.name()).orElse(null));
    }

    @Test
    @DisplayName("Reject invalid group item")
    void rejectsInvalidItem() {
        var result = ctx.parse(GroupBy.class, "GROUP BY -");
        assertFalse(result.ok());
    }
}
