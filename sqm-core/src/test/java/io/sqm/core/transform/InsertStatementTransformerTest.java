package io.sqm.core.transform;

import io.sqm.core.InsertStatement;
import io.sqm.core.Node;
import io.sqm.core.Table;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.id;
import static io.sqm.dsl.Dsl.insert;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.row;
import static io.sqm.dsl.Dsl.set;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
            .ignore()
            .values(row(lit(1)))
            .build();

        var transformed = (InsertStatement) new RecursiveNodeTransformer() {
            @Override
            public Node visitLiteralExpr(io.sqm.core.LiteralExpr literalExpr) {
                return lit(2);
            }
        }.transform(statement);

        assertNotSame(statement, transformed);
        assertEquals(InsertStatement.InsertMode.IGNORE, transformed.insertMode());
    }

    @Test
    void rebuildsStatementWhenReturningChanges() {
        var statement = insert(tbl("users"))
            .values(row(lit(1)))
            .result(col("id").toSelectItem())
            .build();

        Node transformed = new RecursiveNodeTransformer() {
            @Override
            public Node visitColumnExpr(io.sqm.core.ColumnExpr columnExpr) {
                return col("new_id");
            }
        }.transform(statement);

        assertNotSame(statement, transformed);
    }

    @Test
    void rebuildsStatementWhenConflictClauseChanges() {
        var statement = insert(tbl("users"))
            .values(row(lit(1)))
            .onConflictDoUpdate(java.util.List.of(id("id")), java.util.List.of(set("name", lit("alice"))), null)
            .build();

        Node transformed = new RecursiveNodeTransformer() {
            @Override
            public Node visitLiteralExpr(io.sqm.core.LiteralExpr literalExpr) {
                if (literalExpr.value() instanceof String) {
                    return lit("alice2");
                }
                return literalExpr;
            }
        }.transform(statement);

        assertNotSame(statement, transformed);
    }
}
