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
        assertEquals(1, composedQuery.orderBy().items().get(0).ordinal());
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
        assertEquals(SetOperator.UNION, composedQuery.ops().get(0));
    }

    @Test
    void unionAll() {
        var query1 = Query.select(Expression.literal(1));
        var query2 = Query.select(Expression.literal(2));
        var composedQuery = query1.unionAll(query2);
        assertInstanceOf(CompositeQuery.class, composedQuery);
        assertEquals(SetOperator.UNION_ALL, composedQuery.ops().get(0));
    }

    @Test
    void intersect() {
        var query1 = Query.select(Expression.literal(1));
        var query2 = Query.select(Expression.literal(2));
        var composedQuery = query1.intersect(query2);
        assertInstanceOf(CompositeQuery.class, composedQuery);
        assertEquals(SetOperator.INTERSECT, composedQuery.ops().get(0));
    }

    @Test
    void intersectAll() {
        var query1 = Query.select(Expression.literal(1));
        var query2 = Query.select(Expression.literal(2));
        var composedQuery = query1.intersectAll(query2);
        assertInstanceOf(CompositeQuery.class, composedQuery);
        assertEquals(SetOperator.INTERSECT_ALL, composedQuery.ops().get(0));
    }

    @Test
    void except() {
        var query1 = Query.select(Expression.literal(1));
        var query2 = Query.select(Expression.literal(2));
        var composedQuery = query1.except(query2);
        assertInstanceOf(CompositeQuery.class, composedQuery);
        assertEquals(SetOperator.EXCEPT, composedQuery.ops().get(0));
    }

    @Test
    void exceptAll() {
        var query1 = Query.select(Expression.literal(1));
        var query2 = Query.select(Expression.literal(2));
        var composedQuery = query1.exceptAll(query2);
        assertInstanceOf(CompositeQuery.class, composedQuery);
        assertEquals(SetOperator.EXCEPT_ALL, composedQuery.ops().get(0));
    }

    @Test
    void asSelect() {
        Query query1 = Query.select(Expression.literal(1));
        Query query2 = Query.select(Expression.literal(2));
        assertTrue(query1.asSelect().isPresent());
        assertFalse(query1.union(query2).asSelect().isPresent());
    }

    @Test
    void asWith() {
        var cte = Query.cte("name");
        var query = Query.select(Expression.literal(1));
        assertTrue(Query.with(List.of(cte), query).asWith().isPresent());
        assertFalse(query.asWith().isPresent());
    }

    @Test
    void asComposite() {
        Query query1 = Query.select(Expression.literal(1));
        Query query2 = Query.select(Expression.literal(2));
        assertTrue(query1.union(query2).asComposite().isPresent());
        assertFalse(query1.asComposite().isPresent());
    }
}