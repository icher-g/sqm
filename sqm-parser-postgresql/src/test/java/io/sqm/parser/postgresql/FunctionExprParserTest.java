package io.sqm.parser.postgresql;

import io.sqm.core.ColumnExpr;
import io.sqm.core.Direction;
import io.sqm.core.FunctionExpr;
import io.sqm.core.ExprSelectItem;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FunctionExprParserTest {
    private final ParseContext ctx = ParseContext.of(new PostgresSpecs());

    @Test
    void parsesAggregateInputOrderBy() {
        var result = ctx.parse(FunctionExpr.class, "ARRAY_AGG(DISTINCT o.sales_channel ORDER BY o.sales_channel)");

        assertTrue(result.ok(), () -> "expected ok, got error: " + result.errorMessage());
        var function = result.value();
        assertEquals("ARRAY_AGG", function.name().values().getFirst());
        assertTrue(function.distinctArg());
        assertEquals(1, function.args().size());
        assertNotNull(function.orderBy());
        assertEquals(1, function.orderBy().items().size());
        assertInstanceOf(ColumnExpr.class, function.orderBy().items().getFirst().expr());
    }

    @Test
    void parsesAggregateInputOrderByInSelectQuery() {
        var result = ctx.parse(Query.class, """
            SELECT
                o.customer_id,
                ARRAY_AGG(DISTINCT o.sales_channel ORDER BY o.sales_channel) AS channels
            FROM orders o
            GROUP BY o.customer_id
            """);

        assertTrue(result.ok(), () -> "expected ok, got error: " + result.errorMessage());
        var query = assertInstanceOf(SelectQuery.class, result.value());
        var item = assertInstanceOf(ExprSelectItem.class, query.items().get(1));
        var function = assertInstanceOf(FunctionExpr.class, item.expr());
        assertNotNull(function.orderBy());
    }

    @Test
    void parsesMultiArgumentAggregateInputOrderBy() {
        var result = ctx.parse(FunctionExpr.class, "STRING_AGG(name, ',' ORDER BY name DESC)");

        assertTrue(result.ok(), () -> "expected ok, got error: " + result.errorMessage());
        var function = result.value();
        assertEquals(2, function.args().size());
        assertNotNull(function.orderBy());
        assertEquals(1, function.orderBy().items().size());
        assertEquals(Direction.DESC, function.orderBy().items().getFirst().direction());
    }
}
