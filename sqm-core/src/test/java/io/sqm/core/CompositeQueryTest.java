package io.sqm.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CompositeQueryTest {

    @Test
    void of() {
        List<Query> terms = List.of(Query.select(Expression.literal(1)).build(), Query.select(Expression.literal(2)).build());
        OrderBy orderBy = OrderBy.of(OrderItem.of(1));
        LimitOffset limitOffset = LimitOffset.of(1L, 1L);
        assertInstanceOf(CompositeQuery.class, CompositeQuery.of(terms, List.of(SetOperator.UNION), orderBy, limitOffset));
    }

    @Test
    void orderBy() {
        List<Query> terms = List.of(Query.select(Expression.literal(1)).build(), Query.select(Expression.literal(2)).build());
        CompositeQuery query = CompositeQuery.of(terms, List.of(SetOperator.UNION)).orderBy(OrderItem.of(1));
        assertNotNull(query.orderBy());
        assertEquals(1, query.orderBy().items().size());
        query = CompositeQuery.of(terms, List.of(SetOperator.UNION)).orderBy(List.of(OrderItem.of(2)));
        assertNotNull(query.orderBy());
        assertEquals(1, query.orderBy().items().size());
    }

    @Test
    void limit() {
        List<Query> terms = List.of(Query.select(Expression.literal(1)).build(), Query.select(Expression.literal(2)).build());
        CompositeQuery query = CompositeQuery.of(terms, List.of(SetOperator.UNION)).limit(1L);
        assertNotNull(query.limitOffset());
        assertInstanceOf(LiteralExpr.class, query.limitOffset().limit());
        assertEquals(1L, ((LiteralExpr) query.limitOffset().limit()).value());
        // query with offset
        query = CompositeQuery.of(terms, List.of(SetOperator.UNION)).offset(2L).limit(1L);
        assertNotNull(query.limitOffset());
        assertInstanceOf(LiteralExpr.class, query.limitOffset().limit());
        assertEquals(1L, ((LiteralExpr) query.limitOffset().limit()).value());
    }

    @Test
    void offset() {
        List<Query> terms = List.of(Query.select(Expression.literal(1)).build(), Query.select(Expression.literal(2)).build());
        CompositeQuery query = CompositeQuery.of(terms, List.of(SetOperator.UNION)).offset(1L);
        assertNotNull(query.limitOffset());
        assertInstanceOf(LiteralExpr.class, query.limitOffset().offset());
        assertEquals(1L, ((LiteralExpr) query.limitOffset().offset()).value());
        // query with limit
        query = CompositeQuery.of(terms, List.of(SetOperator.UNION)).limit(2L).offset(1L);
        assertNotNull(query.limitOffset());
        assertInstanceOf(LiteralExpr.class, query.limitOffset().offset());
        assertEquals(1L, ((LiteralExpr) query.limitOffset().offset()).value());
    }

    @Test
    void limitExpressionAndNullLimit() {
        List<Query> terms = List.of(Query.select(Expression.literal(1)).build(), Query.select(Expression.literal(2)).build());
        CompositeQuery query = CompositeQuery.of(terms, List.of(SetOperator.UNION)).limit(Expression.literal(3));
        assertNotNull(query.limitOffset());
        assertInstanceOf(LiteralExpr.class, query.limitOffset().limit());
        assertEquals(3, ((LiteralExpr) query.limitOffset().limit()).value());

        query = CompositeQuery.of(terms, List.of(SetOperator.UNION)).limit((Long) null);
        assertNotNull(query.limitOffset());
        assertNull(query.limitOffset().limit());
    }

    @Test
    void offsetExpressionAndNullOffset() {
        List<Query> terms = List.of(Query.select(Expression.literal(1)).build(), Query.select(Expression.literal(2)).build());
        CompositeQuery query = CompositeQuery.of(terms, List.of(SetOperator.UNION)).offset(Expression.literal(4));
        assertNotNull(query.limitOffset());
        assertInstanceOf(LiteralExpr.class, query.limitOffset().offset());
        assertEquals(4, ((LiteralExpr) query.limitOffset().offset()).value());

        query = CompositeQuery.of(terms, List.of(SetOperator.UNION)).offset((Long) null);
        assertNotNull(query.limitOffset());
        assertNull(query.limitOffset().offset());
    }
}
