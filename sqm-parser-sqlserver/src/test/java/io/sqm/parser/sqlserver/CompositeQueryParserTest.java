package io.sqm.parser.sqlserver;

import io.sqm.core.CompositeQuery;
import io.sqm.core.Query;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.sqlserver.spi.SqlServerSpecs;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompositeQueryParserTest {
    private final ParseContext ctx = ParseContext.of(new SqlServerSpecs());

    @Test
    void globalOffsetFetchRequiresGlobalOrdering() {
        var valid = ctx.parse(Query.class,
            "SELECT id FROM users UNION SELECT id FROM orders ORDER BY id OFFSET 2 ROWS FETCH NEXT 5 ROWS ONLY");
        assertTrue(valid.ok(), valid::errorMessage);
        assertNotNull(assertInstanceOf(CompositeQuery.class, valid.value()).orderBy());
        for (var sql : new String[]{
            "SELECT 1 UNION SELECT 2 OFFSET 2 ROWS",
            "SELECT 1 UNION (SELECT 2 ORDER BY 1) OFFSET 2 ROWS",
            "SELECT 1 UNION SELECT 2 LIMIT 5",
            "SELECT 1 UNION SELECT 2 ORDER BY 1 FETCH NEXT 5 ROWS ONLY",
            "SELECT 1 UNION"}) {
            assertFalse(ctx.parse(Query.class, sql).ok(), sql);
        }
    }

    @Test
    void simpleAndUnpaginatedQueriesStillParse() {
        assertTrue(ctx.parse(Query.class, "SELECT 1").ok());
        assertTrue(ctx.parse(Query.class, "SELECT 1 UNION SELECT 2").ok());
        assertTrue(ctx.parse(Query.class,
            "SELECT 1 UNION (SELECT 2 ORDER BY 1 OFFSET 2 ROWS)").ok());
    }
}
