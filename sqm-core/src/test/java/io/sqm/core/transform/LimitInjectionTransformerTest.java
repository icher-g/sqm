package io.sqm.core.transform;

import io.sqm.core.*;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class LimitInjectionTransformerTest {

    @Test
    void injects_limit_when_absent() {
        SelectQuery query = select(col("u", "id")).from(tbl("users").as("u"));

        var transformed = (SelectQuery) LimitInjectionTransformer.of(100).apply(query);

        assertEquals(100L, ((LiteralExpr) transformed.limit()).value());
        assertNull(transformed.offset());
    }

    @Test
    void keeps_existing_limit_unchanged() {
        SelectQuery query = select(col("u", "id")).from(tbl("users").as("u")).limit(10);

        var transformed = (SelectQuery) LimitInjectionTransformer.of(100).apply(query);

        assertEquals(10L, ((LiteralExpr) transformed.limit()).value());
    }

    @Test
    void injects_limit_and_preserves_offset() {
        SelectQuery query = select(col("u", "id")).from(tbl("users").as("u")).offset(5);

        var transformed = (SelectQuery) LimitInjectionTransformer.of(100).apply(query);

        assertEquals(100L, ((LiteralExpr) transformed.limit()).value());
        assertEquals(5L, ((LiteralExpr) transformed.offset()).value());
    }

    @Test
    void does_not_override_limit_all() {
        SelectQuery query = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .limitOffset(LimitOffset.all());

        var transformed = (SelectQuery) LimitInjectionTransformer.of(100).apply(query);

        assertSame(query, transformed);
        assertTrue(transformed.limitOffset().limitAll());
        assertNull(transformed.limit());
    }

    @Test
    void injects_into_composite_query_when_absent() {
        CompositeQuery query = select(lit(1)).union(select(lit(2)));

        var transformed = (CompositeQuery) LimitInjectionTransformer.of(50).apply(query);

        assertEquals(50L, ((LiteralExpr) transformed.limitOffset().limit()).value());
    }

    @Test
    void injects_into_with_query_body() {
        Query query = with(Query.cte("u", select(lit(1)))).body(select(lit(1)));

        var transformed = LimitInjectionTransformer.of(25).apply(query);

        var withQuery = (WithQuery) transformed;
        var body = (SelectQuery) withQuery.body();
        assertEquals(25L, ((LiteralExpr) body.limit()).value());
    }

    @Test
    void rejects_non_positive_default_limit() {
        assertThrows(IllegalArgumentException.class, () -> LimitInjectionTransformer.of(0));
    }

    @Test
    void rejects_null_default_limit_expression() {
        assertThrows(IllegalArgumentException.class, () -> LimitInjectionTransformer.of(null));
    }
}
