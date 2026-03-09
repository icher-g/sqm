package io.sqm.core.walk;

import io.sqm.core.Assignment;
import io.sqm.core.Table;
import io.sqm.core.UpdateStatement;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.set;
import static io.sqm.dsl.Dsl.tbl;
import static io.sqm.dsl.Dsl.update;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UpdateStatementVisitorTest {

    @Test
    void recursiveVisitorTraversesUpdateChildren() {
        var statement = update(tbl("users"))
            .set(set("name", lit("alice")))
            .from(tbl("source_users"))
            .where(col("id").eq(lit(1)))
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
        }.accept(statement);

        assertEquals(List.of("update", "table", "assignment", "table"), visits.subList(0, 4));
    }
}
