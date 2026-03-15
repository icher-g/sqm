package io.sqm.render.sqlserver;

import io.sqm.core.Expression;
import io.sqm.core.OrderItem;
import io.sqm.core.Query;
import io.sqm.core.QuoteStyle;
import io.sqm.core.SelectQuery;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.sqlserver.spi.SqlServerDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.distinct;
import static io.sqm.dsl.Dsl.id;
import static io.sqm.dsl.Dsl.top;
import static io.sqm.dsl.Dsl.tbl;

class SqlServerSelectQueryRendererTest {

    @Test
    void renders_distinct_top_query() {
        var query = SelectQuery.builder()
            .distinct(distinct())
            .top(top(10))
            .select(col(id("u", QuoteStyle.BRACKETS), id("id", QuoteStyle.BRACKETS)))
            .from(tbl(id("users", QuoteStyle.BRACKETS)).as(id("u", QuoteStyle.BRACKETS)))
            .build();

        var rendered = RenderContext.of(new SqlServerDialect()).render(query);

        assertEquals("SELECT DISTINCT TOP (10) [u].[id] FROM [users] AS [u]", normalize(rendered.sql()));
    }

    @Test
    void renders_order_by_offset_fetch_query() {
        var query = SelectQuery.builder()
            .select(Expression.literal(1))
            .orderBy(OrderItem.of(1))
            .limitOffset(io.sqm.core.LimitOffset.of(Expression.literal(10), Expression.literal(5)))
            .build();

        var rendered = RenderContext.of(new SqlServerDialect()).render(query);

        assertEquals("SELECT 1 ORDER BY 1 OFFSET 5 ROWS FETCH NEXT 10 ROWS ONLY", normalize(rendered.sql()));
    }

    @Test
    void rejects_offset_fetch_without_order_by() {
        var query = Query.select(Expression.literal(1))
            .limitOffset(io.sqm.core.LimitOffset.of(Expression.literal(10), Expression.literal(5)))
            .build();

        assertThrows(UnsupportedOperationException.class, () -> RenderContext.of(new SqlServerDialect()).render(query));
    }

    @Test
    void renders_top_percent_when_top_spec_requests_it() {
        var query = SelectQuery.builder()
            .top(io.sqm.dsl.Dsl.topPercent(Expression.literal(10)))
            .select(Expression.literal(1))
            .build();

        var rendered = RenderContext.of(new SqlServerDialect()).render(query);

        assertEquals("SELECT TOP (10) PERCENT 1", normalize(rendered.sql()));
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
