package io.sqm.core.transform;

import io.sqm.core.LiteralExpr;
import io.sqm.core.Node;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.set;
import static io.sqm.dsl.Dsl.tbl;
import static io.sqm.dsl.Dsl.update;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class UpdateStatementTransformerTest {

    @Test
    void preservesIdentityWhenChildrenDoNotChange() {
        var statement = update(tbl("users"))
            .set(set("name", lit("alice")))
            .build();

        Node transformed = new RecursiveNodeTransformer() {
        }.transform(statement);

        assertSame(statement, transformed);
    }

    @Test
    void rebuildsStatementWhenAssignmentChanges() {
        var statement = update(tbl("users"))
            .set(set("name", lit("alice")))
            .build();

        Node transformed = new RecursiveNodeTransformer() {
            @Override
            public Node visitLiteralExpr(LiteralExpr literalExpr) {
                return lit("bob");
            }
        }.transform(statement);

        assertNotSame(statement, transformed);
    }
}
