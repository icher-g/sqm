package io.sqm.parser.mysql;

import io.sqm.core.GroupBy;
import io.sqm.core.GroupItem;
import io.sqm.parser.mysql.spi.MySqlSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupByParserTest {

    private final ParseContext ctx = ParseContext.of(new MySqlSpecs());

    @Test
    void parsesGroupByWithRollupKeywordForm() {
        var result = ctx.parse(GroupBy.class, "GROUP BY department, status WITH ROLLUP");

        assertTrue(result.ok());
        assertEquals(1, result.value().items().size());
        var rollup = (GroupItem.Rollup) result.value().items().getFirst();
        assertEquals(2, rollup.items().size());
    }

    @Test
    void parsesRegularGroupByWithoutRollup() {
        var result = ctx.parse(GroupBy.class, "GROUP BY department, status");

        assertTrue(result.ok());
        assertEquals(2, result.value().items().size());
    }

    @Test
    void rejectsWithWithoutRollup_withDeterministicMessage() {
        var result = ctx.parse(GroupBy.class, "GROUP BY department WITH CUBE");

        assertFalse(result.ok());
        assertNotNull(result.errorMessage());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Expected ROLLUP after WITH"));
    }

    @Test
    void rejectsGroupByWithoutItems() {
        var result = ctx.parse(GroupBy.class, "GROUP BY");

        assertTrue(result.isError());
    }

    @Test
    void targetTypeIsGroupBy() {
        assertEquals(GroupBy.class, new GroupByParser().targetType());
    }
}
