package io.sqm.core.transform;

import io.sqm.core.Node;
import io.sqm.core.Table;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.insert;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.row;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class InsertStatementTransformerTest {

    @Test
    void preservesIdentityWhenChildrenDoNotChange() {
        var statement = insert(tbl("users"))
            .values(row(lit(1)))
            .build();

        Node transformed = new RecursiveNodeTransformer() {
        }.transform(statement);

        assertSame(statement, transformed);
    }

    @Test
    void rebuildsStatementWhenTargetChanges() {
        var statement = insert(tbl("users"))
            .values(row(lit(1)))
            .build();

        Node transformed = new RecursiveNodeTransformer() {
            @Override
            public Node visitTable(Table table) {
                return table.inSchema("public");
            }
        }.transform(statement);

        assertNotSame(statement, transformed);
    }

    @Test
    void rebuildsStatementWhenSourceChanges() {
        var statement = insert(tbl("users"))
            .values(row(lit(1)))
            .build();

        Node transformed = new RecursiveNodeTransformer() {
            @Override
            public Node visitLiteralExpr(io.sqm.core.LiteralExpr literalExpr) {
                return lit(2);
            }
        }.transform(statement);

        assertNotSame(statement, transformed);
    }

    @Test
    void rebuildsStatementWhenReturningChanges() {
        var statement = insert(tbl("users"))
            .values(row(lit(1)))
            .returning(col("id").toSelectItem())
            .build();

        Node transformed = new RecursiveNodeTransformer() {
            @Override
            public Node visitColumnExpr(io.sqm.core.ColumnExpr columnExpr) {
                return col("new_id");
            }
        }.transform(statement);

        assertNotSame(statement, transformed);
    }
}
