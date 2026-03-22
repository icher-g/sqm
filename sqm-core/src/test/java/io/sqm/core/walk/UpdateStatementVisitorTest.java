package io.sqm.core.walk;

import io.sqm.core.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateStatementVisitorTest {

    @Test
    void recursiveVisitorTraversesUpdateChildren() {
        var statement = update(tbl("users"))
            .join(inner(tbl("orders")).on(col("users", "id").eq(col("orders", "user_id"))))
            .set(set("name", lit("alice")))
            .from(tbl("source_users"))
            .where(col("id").eq(lit(1)))
            .result(resultInto(tableVar("audit_rows"), "name"), deleted("name"), inserted("name"))
            .build();
        var visits = new ArrayList<String>();

        new RecursiveNodeVisitor<Void>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitUpdateStatement(UpdateStatement updateStatement) {
                visits.add("update");
                return super.visitUpdateStatement(updateStatement);
            }

            @Override
            public Void visitAssignment(Assignment assignment) {
                visits.add("assignment");
                return super.visitAssignment(assignment);
            }

            @Override
            public Void visitTable(Table table) {
                visits.add("table");
                return super.visitTable(table);
            }

            @Override
            public Void visitVariableTableRef(VariableTableRef tableVariable) {
                visits.add("tableVar");
                return super.visitVariableTableRef(tableVariable);
            }
        }.accept(statement);

        assertEquals(List.of("update", "table", "assignment", "table", "table"), visits.subList(0, 5));
        assertTrue(visits.contains("tableVar"));
        assertEquals(OutputRowSource.DELETED, statement.result().items().getFirst().matchResultItem().expr(e -> e.expr().matchExpression().outputColumn(OutputColumnExpr::source).orElse(null)).orElse(null));
    }
}
