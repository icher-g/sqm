package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QueryExprTest {

    @Test
    void of() {
        var subquery = Query.select(Expression.literal(1)).build();
        var queryExpr = QueryExpr.of(subquery);

        assertNotNull(queryExpr);
        assertInstanceOf(QueryExpr.class, queryExpr);
        assertEquals(subquery, queryExpr.subquery());
    }

    @Test
    void subquery() {
        var subquery = Query.select(Expression.column("id")).from(TableRef.table("users")).build();
        var queryExpr = QueryExpr.of(subquery);
        assertEquals(subquery, queryExpr.subquery());
    }

    @Test
    void accept() {
        var queryExpr = QueryExpr.of(Query.select(Expression.literal(1)).build());
        var visitor = new TestVisitor();
        var result = queryExpr.accept(visitor);
        assertTrue(result);
    }

    @Test
    void withComplexQuery() {
        var subquery = Query.select(Expression.column("id")).from(TableRef.table("orders"))
            .where(Expression.column("status").eq("active")).build();
        var queryExpr = QueryExpr.of(subquery);
        assertNotNull(queryExpr.subquery());
    }

    @Test
    void implementsValueSet() {
        var queryExpr = QueryExpr.of(Query.select(Expression.literal(1)).build());
        assertInstanceOf(ValueSet.class, queryExpr);
    }

    static class TestVisitor extends io.sqm.core.walk.RecursiveNodeVisitor<Boolean> {
        @Override
        protected Boolean defaultResult() {
            return false;
        }

        @Override
        public Boolean visitQueryExpr(QueryExpr node) {
            return true;
        }
    }
}
