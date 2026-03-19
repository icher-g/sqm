package io.sqm.core.walk;

import io.sqm.core.Assignment;
import io.sqm.core.ExprResultItem;
import io.sqm.core.InsertStatement;
import io.sqm.core.Table;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InsertStatementVisitorTest {

    @Test
    void recursiveVisitorTraversesInsertChildren() {
        var statement = insert(tbl("users"))
            .values(row(lit(1), lit("alice")))
            .onConflictDoUpdate(java.util.List.of(io.sqm.core.Identifier.of("id")), java.util.List.of(set("name", lit("alice2"))), col("id").eq(lit(1)))
            .result(inserted("id"))
            .build();
        var visits = new ArrayList<String>();

        new RecursiveNodeVisitor<Void>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitInsertStatement(InsertStatement insertStatement) {
                visits.add("insert");
                return super.visitInsertStatement(insertStatement);
            }

            @Override
            public Void visitTable(Table table) {
                visits.add("table");
                return super.visitTable(table);
            }

            @Override
            public Void visitRowExpr(io.sqm.core.RowExpr rowExpr) {
                visits.add("row");
                return super.visitRowExpr(rowExpr);
            }

            @Override
            public Void visitAssignment(Assignment assignment) {
                visits.add("conflict-assignment");
                return super.visitAssignment(assignment);
            }

            @Override
            public Void visitExprResultItem(ExprResultItem item) {
                visits.add("result");
                return super.visitExprResultItem(item);
            }
        }.accept(statement);

        assertEquals(List.of("insert", "table", "row", "conflict-assignment", "result"), visits.subList(0, 5));
    }
}
