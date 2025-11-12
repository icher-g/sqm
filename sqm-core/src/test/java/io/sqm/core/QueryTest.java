package io.sqm.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QueryTest {

    @Test
    void select() {
        assertInstanceOf(SelectQuery.class, Query.select(Expression.literal(1)));
    }

    @Test
    void compose() {
        var query1 = Query.select(Expression.literal(1));
        var query2 = Query.select(Expression.literal(2));
        var composedQuery = Query.compose(List.of(query1, query2), List.of(SetOperator.UNION));
        assertInstanceOf(CompositeQuery.class, composedQuery);
        assertEquals(2, composedQuery.terms().size());
        composedQuery = Query.compose(List.of(query1, query2), List.of(SetOperator.UNION), OrderBy.of(OrderItem.of(1)), LimitOffset.of(1L, 2L));
        assertInstanceOf(CompositeQuery.class, composedQuery);
        assertEquals(2, composedQuery.terms().size());
        assertEquals(1, composedQuery.orderBy().items().size());
        assertEquals(1, composedQuery.orderBy().items().getFirst().ordinal());
        assertEquals(1, composedQuery.limitOffset().limit());
        assertEquals(2, composedQuery.limitOffset().offset());
    }

    @Test
    void cte() {
        assertInstanceOf(CteDef.class, Query.cte("name"));
        assertInstanceOf(CteDef.class, Query.cte("name", Query.select(Expression.literal(1))));
        assertInstanceOf(CteDef.class, Query.cte("name", Query.select(Expression.literal(1)), List.of("a")));
    }

    @Test
    void with() {
        var cte = Query.cte("name");
        var query = Query.select(Expression.literal(1));
        assertInstanceOf(WithQuery.class, Query.with(List.of(cte), query));
        assertInstanceOf(WithQuery.class, Query.with(List.of(cte), query, true));
    }

    @Test
    void union() {
        var query1 = Query.select(Expression.literal(1));
        var query2 = Query.select(Expression.literal(2));
        var composedQuery = query1.union(query2);
        assertInstanceOf(CompositeQuery.class, composedQuery);
        assertEquals(SetOperator.UNION, composedQuery.ops().getFirst());
    }

    @Test
    void unionAll() {
        var query1 = Query.select(Expression.literal(1));
        var query2 = Query.select(Expression.literal(2));
        var composedQuery = query1.unionAll(query2);
        assertInstanceOf(CompositeQuery.class, composedQuery);
        assertEquals(SetOperator.UNION_ALL, composedQuery.ops().getFirst());
    }

    @Test
    void intersect() {
        var query1 = Query.select(Expression.literal(1));
        var query2 = Query.select(Expression.literal(2));
        var composedQuery = query1.intersect(query2);
        assertInstanceOf(CompositeQuery.class, composedQuery);
        assertEquals(SetOperator.INTERSECT, composedQuery.ops().getFirst());
    }

    @Test
    void intersectAll() {
        var query1 = Query.select(Expression.literal(1));
        var query2 = Query.select(Expression.literal(2));
        var composedQuery = query1.intersectAll(query2);
        assertInstanceOf(CompositeQuery.class, composedQuery);
        assertEquals(SetOperator.INTERSECT_ALL, composedQuery.ops().getFirst());
    }

    @Test
    void except() {
        var query1 = Query.select(Expression.literal(1));
        var query2 = Query.select(Expression.literal(2));
        var composedQuery = query1.except(query2);
        assertInstanceOf(CompositeQuery.class, composedQuery);
        assertEquals(SetOperator.EXCEPT, composedQuery.ops().getFirst());
    }

    @Test
    void exceptAll() {
        var query1 = Query.select(Expression.literal(1));
        var query2 = Query.select(Expression.literal(2));
        var composedQuery = query1.exceptAll(query2);
        assertInstanceOf(CompositeQuery.class, composedQuery);
        assertEquals(SetOperator.EXCEPT_ALL, composedQuery.ops().getFirst());
    }

    @Test
    void maybeSelect() {
        Query query1 = Query.select(Expression.literal(1));
        Query query2 = Query.select(Expression.literal(2));
        assertTrue(query1.<Boolean>matchQuery().select(s -> true).orElse(false));
        assertFalse(query1.union(query2).<Boolean>matchQuery().select(s -> true).orElse(false));
    }

    @Test
    void maybeWith() {
        var cte = Query.cte("name");
        var query = Query.select(Expression.literal(1));
        assertTrue(Query.with(List.of(cte), query).<Boolean>matchQuery().with(w -> true).orElse(false));
        assertFalse(query.<Boolean>matchQuery().with(w -> true).orElse(false));
    }

    @Test
    void maybeComposite() {
        Query query1 = Query.select(Expression.literal(1));
        Query query2 = Query.select(Expression.literal(2));
        assertTrue(query1.union(query2).<Boolean>matchQuery().composite(c -> true).orElse(false));
        assertFalse(query1.<Boolean>matchQuery().composite(c -> true).orElse(false));
    }
}