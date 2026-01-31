package io.sqm.parser.postgresql;

import io.sqm.core.GroupBy;
import io.sqm.core.GroupItem;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PostgreSQL GroupByParser Tests")
class GroupByParserTest {

    private final ParseContext ctx = ParseContext.of(new PostgresSpecs());

    @Test
    @DisplayName("Parse GROUP BY GROUPING SETS")
    void parsesGroupingSets() {
        var result = parseQuery("SELECT * FROM t GROUP BY GROUPING SETS (a, (a, b), ())");

        assertTrue(result.ok(), () -> "expected ok, got error: " + result.errorMessage());
        var query = assertInstanceOf(SelectQuery.class, result.value());
        GroupBy groupBy = query.groupBy();

        assertNotNull(groupBy);
        assertEquals(1, groupBy.items().size());
        var item = groupBy.items().getFirst();
        assertInstanceOf(GroupItem.GroupingSets.class, item);
        var sets = (GroupItem.GroupingSets) item;
        assertEquals(3, sets.sets().size());
    }

    @Test
    @DisplayName("Parse GROUP BY ROLLUP")
    void parsesRollup() {
        var result = parseQuery("SELECT * FROM t GROUP BY ROLLUP (a, b)");

        assertTrue(result.ok(), () -> "expected ok, got error: " + result.errorMessage());
        var query = assertInstanceOf(SelectQuery.class, result.value());
        var item = query.groupBy().items().getFirst();

        assertInstanceOf(GroupItem.Rollup.class, item);
        assertEquals(2, ((GroupItem.Rollup) item).items().size());
    }

    @Test
    @DisplayName("Parse GROUP BY CUBE")
    void parsesCube() {
        var result = parseQuery("SELECT * FROM t GROUP BY CUBE (a, b)");

        assertTrue(result.ok(), () -> "expected ok, got error: " + result.errorMessage());
        var query = assertInstanceOf(SelectQuery.class, result.value());
        var item = query.groupBy().items().getFirst();

        assertInstanceOf(GroupItem.Cube.class, item);
        assertEquals(2, ((GroupItem.Cube) item).items().size());
    }

    @Test
    @DisplayName("Parse GROUP BY (a, b)")
    void parsesGroupingSet() {
        var result = parseQuery("SELECT * FROM t GROUP BY (a, b)");

        assertTrue(result.ok(), () -> "expected ok, got error: " + result.errorMessage());
        var query = assertInstanceOf(SelectQuery.class, result.value());
        var item = query.groupBy().items().getFirst();

        assertInstanceOf(GroupItem.GroupingSet.class, item);
        assertEquals(2, ((GroupItem.GroupingSet) item).items().size());
    }

    @Test
    @DisplayName("Reject empty ROLLUP")
    void rejectsEmptyRollup() {
        var result = parseQuery("SELECT * FROM t GROUP BY ROLLUP ()");
        assertFalse(result.ok());
    }

    private ParseResult<? extends Query> parseQuery(String sql) {
        return ctx.parse(Query.class, sql);
    }
}
