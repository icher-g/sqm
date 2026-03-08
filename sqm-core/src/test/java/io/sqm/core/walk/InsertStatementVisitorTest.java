package io.sqm.core.walk;

import io.sqm.core.InsertStatement;
import io.sqm.core.Table;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.dsl.Dsl.insert;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.row;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InsertStatementVisitorTest {

    @Test
    void recursiveVisitorTraversesInsertChildren() {
        var statement = insert(tbl("users"))
            .values(row(lit(1), lit("alice")))
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
        }.accept(statement);

        assertEquals(List.of("insert", "table", "row"), visits);
    }
}
