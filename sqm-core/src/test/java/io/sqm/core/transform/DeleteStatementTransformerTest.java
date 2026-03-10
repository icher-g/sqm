package io.sqm.core.transform;

import io.sqm.core.Node;
import io.sqm.core.Table;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class DeleteStatementTransformerTest {

    @Test
    void preservesIdentityWhenChildrenDoNotChange() {
        var statement = delete(tbl("users"))
            .using(tbl("users"))
            .join(inner(tbl("source_users")).on(col("users", "id").eq(col("source_users", "id"))))
            .build();

        Node transformed = new RecursiveNodeTransformer() {
        }.transform(statement);

        assertSame(statement, transformed);
    }

    @Test
    void rebuildsStatementWhenTargetChanges() {
        var statement = delete(tbl("users")).build();

        Node transformed = new RecursiveNodeTransformer() {
            @Override
            public Node visitTable(Table table) {
                return table.inSchema("public");
            }
        }.transform(statement);

        assertNotSame(statement, transformed);
    }

    @Test
    void rebuildsStatementWhenWhereChanges() {
        var statement = delete(tbl("users")).where(io.sqm.dsl.Dsl.col("id").eq(io.sqm.dsl.Dsl.lit(1))).build();

        Node transformed = new RecursiveNodeTransformer() {
            @Override
            public Node visitLiteralExpr(io.sqm.core.LiteralExpr literalExpr) {
                return io.sqm.dsl.Dsl.lit(2);
            }
        }.transform(statement);

        assertNotSame(statement, transformed);
    }

    @Test
    void rebuildsStatementWhenUsingChanges() {
        var statement = delete(tbl("users"))
            .using(tbl("source_users"))
            .build();

        Node transformed = new RecursiveNodeTransformer() {
            @Override
            public Node visitTable(Table table) {
                if ("source_users".equals(table.name().value())) {
                    return tbl("alt_source");
                }
                return table;
            }
        }.transform(statement);

        assertNotSame(statement, transformed);
    }

    @Test
    void rebuildsStatementWhenJoinChanges() {
        var statement = delete(tbl("users"))
            .using(tbl("users"))
            .join(inner(tbl("source_users")).on(col("users", "id").eq(col("source_users", "id"))))
            .build();

        Node transformed = new RecursiveNodeTransformer() {
            @Override
            public Node visitTable(Table table) {
                if ("source_users".equals(table.name().value())) {
                    return tbl("alt_source");
                }
                return table;
            }
        }.transform(statement);

        assertNotSame(statement, transformed);
    }
}
