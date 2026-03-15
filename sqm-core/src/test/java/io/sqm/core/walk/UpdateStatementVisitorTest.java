package io.sqm.core.walk;

import io.sqm.core.Assignment;
import io.sqm.core.OutputColumnExpr;
import io.sqm.core.OutputRowSource;
import io.sqm.core.Table;
import io.sqm.core.UpdateStatement;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.deleted;
import static io.sqm.dsl.Dsl.inner;
import static io.sqm.dsl.Dsl.inserted;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.output;
import static io.sqm.dsl.Dsl.outputItem;
import static io.sqm.dsl.Dsl.set;
import static io.sqm.dsl.Dsl.tbl;
import static io.sqm.dsl.Dsl.update;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UpdateStatementVisitorTest {

    @Test
    void recursiveVisitorTraversesUpdateChildren() {
        var statement = update(tbl("users"))
            .join(inner(tbl("orders")).on(col("users", "id").eq(col("orders", "user_id"))))
            .set(set("name", lit("alice")))
            .from(tbl("source_users"))
            .where(col("id").eq(lit(1)))
            .output(output(outputItem(deleted("name")), outputItem(inserted("name"))))
            .returning(col("id").toSelectItem())
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

        assertEquals(List.of("update", "table", "assignment", "table", "table"), visits.subList(0, 5));
        assertEquals(OutputRowSource.DELETED, statement.output().items().getFirst().expression().matchExpression().outputColumn(OutputColumnExpr::source).orElse(null));
        assertEquals("id", statement.returning().getFirst().matchSelectItem().expr(e -> e.expr().matchExpression().column(c -> c.name().value()).orElse(null)).orElse(null));
    }
}
