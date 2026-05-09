package io.sqm.core;

import io.sqm.core.transform.RecursiveNodeTransformer;
import io.sqm.core.walk.RecursiveNodeVisitor;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class HierarchicalQueryClauseTest {
    @Test
    void createsHierarchicalClauseAndPriorExpression() {
        var start = col("parent_id").isNull();
        var id = col("id");
        var prior = prior(id);
        var connect = prior.eq(col("parent_id"));
        var order = orderBy(col("name"));
        var clause = hierarchy(start, connect, true, order);

        assertSame(start, clause.startWith());
        assertSame(connect, clause.connectBy());
        assertTrue(clause.noCycle());
        assertSame(order, clause.orderSiblingsBy());
        assertSame(id, prior.expr());
    }

    @Test
    void rejectsMissingConnectByPredicate() {
        assertThrows(NullPointerException.class, () -> hierarchy(null, null, false, null));
        assertThrows(NullPointerException.class, () -> PriorExpr.of(null));
    }

    @Test
    void selectBuilderCopiesHierarchicalClause() {
        var hierarchy = hierarchy(null, prior(col("id")).eq(col("parent_id")), false);

        var query = select(col("id"))
            .from(tbl("categories"))
            .hierarchical(hierarchy)
            .build();
        var copied = SelectQuery.builder(query).build();

        assertSame(hierarchy, query.hierarchical());
        assertEquals(query, copied);
        assertSame(hierarchy, copied.hierarchical());
    }

    @Test
    void visitorAndTransformerTraverseNestedNodes() {
        var clause = hierarchy(col("parent_id").isNull(), prior(col("id")).eq(col("parent_id")), false, orderBy(col("name")));
        var query = select(col("id")).from(tbl("categories")).hierarchical(clause).build();
        var visitor = new RecursiveNodeVisitor<Integer>() {
            private int count;

            @Override
            protected Integer defaultResult() {
                return count;
            }

            @Override
            public Integer visitPriorExpr(PriorExpr expr) {
                count++;
                return super.visitPriorExpr(expr);
            }
        };
        var unchanged = new RecursiveNodeTransformer() {
        };

        assertEquals(1, query.accept(visitor));
        assertSame(query, unchanged.transform(query));
    }

    @Test
    void expressionMatcherMatchesPrior() {
        var out = prior(col("id")).matchExpression()
            .column(column -> "column")
            .prior(prior -> "prior")
            .orElse("other");

        assertEquals("prior", out);
    }
}
