package io.sqm.core.transform;

import io.sqm.core.MergeStatement;
import io.sqm.core.Node;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.id;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.merge;
import static io.sqm.dsl.Dsl.row;
import static io.sqm.dsl.Dsl.set;
import static io.sqm.dsl.Dsl.tbl;
import static io.sqm.dsl.Dsl.top;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class MergeStatementTransformerTest {

    @Test
    void preservesIdentityWhenChildrenDoNotChange() {
        var statement = merge(tbl("users"))
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedDelete()
            .build();

        Node transformed = new RecursiveNodeTransformer() {
        }.transform(statement);

        assertSame(statement, transformed);
    }

    @Test
    void rebuildsStatementWhenMatchPredicateChanges() {
        var statement = merge(tbl("users"))
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(lit(1)))
            .whenMatchedDelete()
            .build();

        var transformed = (MergeStatement) new RecursiveNodeTransformer() {
            @Override
            public Node visitLiteralExpr(io.sqm.core.LiteralExpr literalExpr) {
                return lit(2);
            }
        }.transform(statement);

        assertNotSame(statement, transformed);
        assertEquals(2, transformed.on().matchPredicate()
            .comparison(cmp -> cmp.rhs().matchExpression().literal(l -> l.value()).orElse(null))
            .orElse(null));
    }

    @Test
    void rebuildsStatementWhenInsertActionValuesChange() {
        var statement = merge(tbl("users"))
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenNotMatchedInsert(java.util.List.of(id("id")), row(lit(1)))
            .build();

        var transformed = (MergeStatement) new RecursiveNodeTransformer() {
            @Override
            public Node visitLiteralExpr(io.sqm.core.LiteralExpr literalExpr) {
                return lit(2);
            }
        }.transform(statement);

        assertNotSame(statement, transformed);
        assertEquals(2, transformed.clauses().getFirst().action().matchMergeAction()
            .insert(action -> action.values().items().getFirst().matchExpression().literal(l -> l.value()).orElse(null))
            .orElse(null));
    }

    @Test
    void rebuildsStatementWhenAssignmentsChange() {
        var statement = merge(tbl("users"))
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedUpdate(java.util.List.of(set("name", lit("alice"))))
            .build();

        var transformed = (MergeStatement) new RecursiveNodeTransformer() {
            @Override
            public Node visitLiteralExpr(io.sqm.core.LiteralExpr literalExpr) {
                return lit("bob");
            }
        }.transform(statement);

        assertNotSame(statement, transformed);
        assertEquals("bob", transformed.clauses().getFirst().action().matchMergeAction()
            .update(action -> action.assignments().getFirst().value().matchExpression().literal(l -> l.value()).orElse(null))
            .orElse(null));
    }

    @Test
    void rebuildsStatementWhenClauseConditionChanges() {
        var statement = merge(tbl("users"))
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedDelete(col("s", "active").eq(lit(1)))
            .build();

        var transformed = (MergeStatement) new RecursiveNodeTransformer() {
            @Override
            public Node visitLiteralExpr(io.sqm.core.LiteralExpr literalExpr) {
                return lit(2);
            }
        }.transform(statement);

        assertNotSame(statement, transformed);
        assertEquals(2, transformed.clauses().getFirst().condition().matchPredicate()
            .comparison(cmp -> cmp.rhs().matchExpression().literal(l -> l.value()).orElse(null))
            .orElse(null));
    }

    @Test
    void preservesIdentityForDoNothingAction() {
        var statement = merge(tbl("users"))
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedDoNothing()
            .build();

        var transformed = new RecursiveNodeTransformer() {
        }.transform(statement);

        assertSame(statement, transformed);
    }

    @Test
    void rebuildsStatementWhenTopSpecChanges() {
        var statement = merge(tbl("users"))
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .top(top(1))
            .whenMatchedDelete()
            .build();

        var transformed = (MergeStatement) new RecursiveNodeTransformer() {
            @Override
            public Node visitLiteralExpr(io.sqm.core.LiteralExpr literalExpr) {
                return lit(2);
            }
        }.transform(statement);

        assertNotSame(statement, transformed);
        assertEquals(2, transformed.topSpec().count().matchExpression().literal(l -> l.value()).orElse(null));
    }

    @Test
    void rebuildsStatementWhenHintChanges() {
        var statement = merge(tbl("users"))
            .hint("MAX_EXECUTION_TIME", 1000)
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedDelete()
            .build();

        var transformed = (MergeStatement) new RecursiveNodeTransformer() {
            @Override
            public Node visitLiteralExpr(io.sqm.core.LiteralExpr literalExpr) {
                if (Integer.valueOf(1000).equals(literalExpr.value())) {
                    return lit(2000);
                }
                return literalExpr;
            }
        }.transform(statement);

        assertNotSame(statement, transformed);
        assertEquals(2000, transformed.hints().getFirst().args().getFirst()
            .matchHintArg().expression(arg -> arg.value().matchExpression().literal(l -> l.value()).orElse(null)).otherwise(arg -> null));
    }
}
