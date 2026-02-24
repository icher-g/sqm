package io.sqm.core;

import io.sqm.dsl.Dsl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QueryTest {

    @Test
    void select() {
        assertInstanceOf(SelectQuery.class, Query.select(Expression.literal(1)).build());
    }

    @Test
    void compose() {
        var query1 = Query.select(Expression.literal(1)).build();
        var query2 = Query.select(Expression.literal(2)).build();
        var composedQuery = Query.compose(List.of(query1, query2), List.of(SetOperator.UNION));
        assertInstanceOf(CompositeQuery.class, composedQuery);
        assertEquals(2, composedQuery.terms().size());
        composedQuery = Query.compose(List.of(query1, query2), List.of(SetOperator.UNION), OrderBy.of(OrderItem.of(1)), LimitOffset.of(1L, 2L));
        assertInstanceOf(CompositeQuery.class, composedQuery);
        assertEquals(2, composedQuery.terms().size());
        assertEquals(1, composedQuery.orderBy().items().size());
        assertEquals(1, composedQuery.orderBy().items().getFirst().ordinal());
        assertInstanceOf(LiteralExpr.class, composedQuery.limitOffset().limit());
        assertInstanceOf(LiteralExpr.class, composedQuery.limitOffset().offset());
        assertEquals(1L, ((LiteralExpr) composedQuery.limitOffset().limit()).value());
        assertEquals(2L, ((LiteralExpr) composedQuery.limitOffset().offset()).value());
    }

    @Test
    void cte() {
        assertInstanceOf(CteDef.class, Dsl.cte("name"));
        assertInstanceOf(CteDef.class, Dsl.cte("name", Query.select(Expression.literal(1)).build()));
        assertInstanceOf(CteDef.class, Dsl.cte("name", Query.select(Expression.literal(1)).build(), List.of("a")));
    }

    @Test
    void with() {
        var cte = Dsl.cte("name");
        var query = Query.select(Expression.literal(1)).build();
        assertInstanceOf(WithQuery.class, Query.with(List.of(cte), query));
        assertInstanceOf(WithQuery.class, Query.with(List.of(cte), query, true));
    }

    @Test
    void union() {
        var query1 = Query.select(Expression.literal(1)).build();
        var query2 = Query.select(Expression.literal(2)).build();
        var composedQuery = query1.union(query2);
        assertInstanceOf(CompositeQuery.class, composedQuery);
        assertEquals(SetOperator.UNION, composedQuery.ops().getFirst());
    }

    @Test
    void unionAll() {
        var query1 = Query.select(Expression.literal(1)).build();
        var query2 = Query.select(Expression.literal(2)).build();
        var composedQuery = query1.unionAll(query2);
        assertInstanceOf(CompositeQuery.class, composedQuery);
        assertEquals(SetOperator.UNION_ALL, composedQuery.ops().getFirst());
    }

    @Test
    void intersect() {
        var query1 = Query.select(Expression.literal(1)).build();
        var query2 = Query.select(Expression.literal(2)).build();
        var composedQuery = query1.intersect(query2);
        assertInstanceOf(CompositeQuery.class, composedQuery);
        assertEquals(SetOperator.INTERSECT, composedQuery.ops().getFirst());
    }

    @Test
    void intersectAll() {
        var query1 = Query.select(Expression.literal(1)).build();
        var query2 = Query.select(Expression.literal(2)).build();
        var composedQuery = query1.intersectAll(query2);
        assertInstanceOf(CompositeQuery.class, composedQuery);
        assertEquals(SetOperator.INTERSECT_ALL, composedQuery.ops().getFirst());
    }

    @Test
    void except() {
        var query1 = Query.select(Expression.literal(1)).build();
        var query2 = Query.select(Expression.literal(2)).build();
        var composedQuery = query1.except(query2);
        assertInstanceOf(CompositeQuery.class, composedQuery);
        assertEquals(SetOperator.EXCEPT, composedQuery.ops().getFirst());
    }

    @Test
    void exceptAll() {
        var query1 = Query.select(Expression.literal(1)).build();
        var query2 = Query.select(Expression.literal(2)).build();
        var composedQuery = query1.exceptAll(query2);
        assertInstanceOf(CompositeQuery.class, composedQuery);
        assertEquals(SetOperator.EXCEPT_ALL, composedQuery.ops().getFirst());
    }

    @Test
    void maybeSelect() {
        Query query1 = Query.select(Expression.literal(1)).build();
        Query query2 = Query.select(Expression.literal(2)).build();
        assertTrue(query1.<Boolean>matchQuery().select(s -> true).orElse(false));
        assertFalse(query1.union(query2).<Boolean>matchQuery().select(s -> true).orElse(false));
    }

    @Test
    void maybeWith() {
        var cte = Query.cte(Identifier.of("name"));
        var query = Query.select(Expression.literal(1)).build();
        assertTrue(Query.with(List.of(cte), query).<Boolean>matchQuery().with(w -> true).orElse(false));
        assertFalse(query.<Boolean>matchQuery().with(w -> true).orElse(false));
    }

    @Test
    void maybeComposite() {
        Query query1 = Query.select(Expression.literal(1)).build();
        Query query2 = Query.select(Expression.literal(2)).build();
        assertTrue(query1.union(query2).<Boolean>matchQuery().composite(c -> true).orElse(false));
        assertFalse(query1.<Boolean>matchQuery().composite(c -> true).orElse(false));
    }
}
