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
import static io.sqm.dsl.Dsl.cross;
import static io.sqm.dsl.Dsl.distinct;
import static io.sqm.dsl.Dsl.id;
import static io.sqm.dsl.Dsl.inner;
import static io.sqm.dsl.Dsl.left;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.top;
import static io.sqm.dsl.Dsl.topWithTies;
import static io.sqm.dsl.Dsl.tbl;
import static io.sqm.dsl.Dsl.unary;

class SelectQueryRendererTest {

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

    @Test
    void renders_top_with_ties_when_order_by_is_present() {
        var query = SelectQuery.builder()
            .top(topWithTies(Expression.literal(10)))
            .select(Expression.literal(1))
            .orderBy(OrderItem.of(1))
            .build();

        var rendered = RenderContext.of(new SqlServerDialect()).render(query);

        assertEquals("SELECT TOP (10) WITH TIES 1 ORDER BY 1", normalize(rendered.sql()));
    }

    @Test
    void rejects_top_with_ties_without_order_by() {
        var query = SelectQuery.builder()
            .top(topWithTies(Expression.literal(10)))
            .select(Expression.literal(1))
            .build();

        assertThrows(UnsupportedOperationException.class, () -> RenderContext.of(new SqlServerDialect()).render(query));
    }

    @Test
    void renders_sqlServerTableHints() {
        var query = SelectQuery.builder()
            .select(Expression.literal(1))
            .from(tbl("users").as("u").withUpdLock().withHoldLock())
            .build();

        var rendered = RenderContext.of(new SqlServerDialect()).render(query);

        assertEquals("SELECT 1 FROM users AS u WITH (UPDLOCK, HOLDLOCK)", normalize(rendered.sql()));
    }

    @Test
    void rejects_conflicting_sqlServerTableHints() {
        var query = SelectQuery.builder()
            .select(Expression.literal(1))
            .from(tbl("users").withNoLock().withUpdLock())
            .build();

        assertThrows(UnsupportedOperationException.class, () -> RenderContext.of(new SqlServerDialect()).render(query));
    }

    @Test
    void rejects_duplicate_sqlServerTableHints() {
        var query = SelectQuery.builder()
            .select(Expression.literal(1))
            .from(tbl("users").withHoldLock().withHoldLock())
            .build();

        assertThrows(UnsupportedOperationException.class, () -> RenderContext.of(new SqlServerDialect()).render(query));
    }

    @Test
    void renders_crossApply_fromCrossJoinLateral() {
        var query = Query.select(col("u", "id"))
            .from(tbl("users").as("u"))
            .join(cross(tbl(Query.select(col("id")).from(tbl("users")).build()).as("sq").lateral()))
            .build();

        var rendered = RenderContext.of(new SqlServerDialect()).render(query);

        assertEquals(
            "SELECT u.id FROM users AS u CROSS APPLY ( SELECT id FROM users ) AS sq",
            normalize(rendered.sql())
        );
    }

    @Test
    void renders_crossApply_fromInnerLateralJoin() {
        var query = Query.select(col("u", "id"))
            .from(tbl("users").as("u"))
            .join(inner(tbl(Query.select(col("id")).from(tbl("users")).build()).as("sq").lateral()).on(unary(lit(true))))
            .build();

        var rendered = RenderContext.of(new SqlServerDialect()).render(query);

        assertEquals(
            "SELECT u.id FROM users AS u CROSS APPLY ( SELECT id FROM users ) AS sq",
            normalize(rendered.sql())
        );
    }

    @Test
    void renders_outerApply_fromLeftLateralJoin() {
        var query = Query.select(col("u", "id"))
            .from(tbl("users").as("u"))
            .join(left(tbl(Query.select(col("id")).from(tbl("users")).build()).as("sq").lateral()).on(unary(lit(true))))
            .build();

        var rendered = RenderContext.of(new SqlServerDialect()).render(query);

        assertEquals(
            "SELECT u.id FROM users AS u OUTER APPLY ( SELECT id FROM users ) AS sq",
            normalize(rendered.sql())
        );
    }

    @Test
    void rejects_nonApplyCompatibleLateralJoinShape() {
        var query = Query.select(col("u", "id"))
            .from(tbl("users").as("u"))
            .join(inner(tbl(Query.select(col("id")).from(tbl("users")).build()).as("sq").lateral())
                .on(col("u", "id").eq(col("sq", "id"))))
            .build();

        assertThrows(UnsupportedOperationException.class, () -> RenderContext.of(new SqlServerDialect()).render(query));
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
