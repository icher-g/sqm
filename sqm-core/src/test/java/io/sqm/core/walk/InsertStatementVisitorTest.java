package io.sqm.core.walk;

import io.sqm.core.Assignment;
import io.sqm.core.ExprSelectItem;
import io.sqm.core.InsertStatement;
import io.sqm.core.Table;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.insert;
import static io.sqm.dsl.Dsl.inserted;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.output;
import static io.sqm.dsl.Dsl.outputItem;
import static io.sqm.dsl.Dsl.row;
import static io.sqm.dsl.Dsl.set;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InsertStatementVisitorTest {

    @Test
    void recursiveVisitorTraversesInsertChildren() {
        var statement = insert(tbl("users"))
            .values(row(lit(1), lit("alice")))
            .onConflictDoUpdate(java.util.List.of(io.sqm.core.Identifier.of("id")), java.util.List.of(set("name", lit("alice2"))), col("id").eq(lit(1)))
            .output(output(outputItem(inserted("id"))))
            .returning(col("id").toSelectItem())
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
            public Void visitExprSelectItem(ExprSelectItem i) {
                visits.add("returning");
                return super.visitExprSelectItem(i);
            }

            @Override
            public Void visitOutputItem(io.sqm.core.OutputItem item) {
                visits.add("output");
                return super.visitOutputItem(item);
            }
        }.accept(statement);

        assertEquals(List.of("insert", "table", "row", "conflict-assignment", "output", "returning"), visits.subList(0, 6));
    }
}
