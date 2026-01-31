package io.sqm.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CompositeQueryTest {

    @Test
    void of() {
        List<Query> terms = List.of(Query.select(Expression.literal(1)), Query.select(Expression.literal(2)));
        OrderBy orderBy = OrderBy.of(OrderItem.of(1));
        LimitOffset limitOffset = LimitOffset.of(1L, 1L);
        assertInstanceOf(CompositeQuery.class, CompositeQuery.of(terms, List.of(SetOperator.UNION), orderBy, limitOffset));
    }

    @Test
    void orderBy() {
        List<Query> terms = List.of(Query.select(Expression.literal(1)), Query.select(Expression.literal(2)));
        CompositeQuery query = CompositeQuery.of(terms, List.of(SetOperator.UNION)).orderBy(OrderItem.of(1));
        assertNotNull(query.orderBy());
        assertEquals(1, query.orderBy().items().size());
        query = CompositeQuery.of(terms, List.of(SetOperator.UNION)).orderBy(List.of(OrderItem.of(2)));
        assertNotNull(query.orderBy());
        assertEquals(1, query.orderBy().items().size());
    }

    @Test
    void limit() {
        List<Query> terms = List.of(Query.select(Expression.literal(1)), Query.select(Expression.literal(2)));
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
        List<Query> terms = List.of(Query.select(Expression.literal(1)), Query.select(Expression.literal(2)));
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
}
