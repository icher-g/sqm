package io.sqm.core.walk;

import io.sqm.core.DeleteStatement;
import io.sqm.core.Table;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.delete;
import static io.sqm.dsl.Dsl.inner;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DeleteStatementVisitorTest {

    @Test
    void recursiveVisitorTraversesDeleteChildren() {
        var statement = delete(tbl("users"))
            .using(tbl("users"))
            .join(inner(tbl("source_users")).on(col("users", "id").eq(col("source_users", "user_id"))))
            .where(col("id").eq(lit(1)))
            .returning(col("id").toSelectItem())
            .build();
        var visits = new ArrayList<String>();

        new RecursiveNodeVisitor<Void>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitDeleteStatement(DeleteStatement deleteStatement) {
                visits.add("delete");
                return super.visitDeleteStatement(deleteStatement);
            }

            @Override
            public Void visitTable(Table table) {
                visits.add("table");
                return super.visitTable(table);
            }
        }.accept(statement);

        assertEquals(List.of("delete", "table", "table", "table"), visits.subList(0, 4));
        assertEquals("id", statement.returning().getFirst().matchSelectItem().expr(e -> e.expr().matchExpression().column(c -> c.name().value()).orElse(null)).orElse(null));
    }
}
