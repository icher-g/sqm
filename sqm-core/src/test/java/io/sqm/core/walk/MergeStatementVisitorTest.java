package io.sqm.core.walk;

import io.sqm.core.MergeClause;
import io.sqm.core.MergeDeleteAction;
import io.sqm.core.MergeDoNothingAction;
import io.sqm.core.MergeInsertAction;
import io.sqm.core.LiteralExpr;
import io.sqm.core.MergeStatement;
import io.sqm.core.MergeUpdateAction;
import io.sqm.core.StatementHint;
import io.sqm.core.Table;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.id;
import static io.sqm.dsl.Dsl.merge;
import static io.sqm.dsl.Dsl.row;
import static io.sqm.dsl.Dsl.set;
import static io.sqm.dsl.Dsl.tbl;
import static io.sqm.dsl.Dsl.top;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MergeStatementVisitorTest {

    @Test
    void recursiveVisitorTraversesMergeChildren() {
        var statement = merge(tbl("users"))
            .hint("MERGE_HINT")
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .top(top(5))
            .whenMatchedUpdate(col("s", "active").eq(io.sqm.dsl.Dsl.lit(true)), List.of(set("name", col("s", "name"))))
            .whenMatchedDelete()
            .whenNotMatchedDoNothing()
            .whenNotMatchedInsert(List.of(id("id"), id("name")), row(col("s", "id"), col("s", "name")))
            .build();
        var visits = new ArrayList<String>();

        new RecursiveNodeVisitor<Void>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitMergeStatement(MergeStatement mergeStatement) {
                visits.add("merge");
                return super.visitMergeStatement(mergeStatement);
            }

            @Override
            public Void visitTable(Table table) {
                visits.add("table");
                return super.visitTable(table);
            }

            @Override
            public Void visitStatementHint(StatementHint hint) {
                visits.add("hint");
                return super.visitStatementHint(hint);
            }

            @Override
            public Void visitMergeClause(MergeClause clause) {
                visits.add("clause");
                return super.visitMergeClause(clause);
            }

            @Override
            public Void visitTopSpec(io.sqm.core.TopSpec topSpec) {
                visits.add("top");
                return super.visitTopSpec(topSpec);
            }

            @Override
            public Void visitMergeUpdateAction(MergeUpdateAction action) {
                visits.add("update");
                return super.visitMergeUpdateAction(action);
            }

            @Override
            public Void visitMergeDeleteAction(MergeDeleteAction action) {
                visits.add("delete");
                return super.visitMergeDeleteAction(action);
            }

            @Override
            public Void visitMergeDoNothingAction(MergeDoNothingAction action) {
                visits.add("doNothing");
                return super.visitMergeDoNothingAction(action);
            }

            @Override
            public Void visitMergeInsertAction(MergeInsertAction action) {
                visits.add("insert");
                return super.visitMergeInsertAction(action);
            }

            @Override
            public Void visitLiteralExpr(LiteralExpr literalExpr) {
                visits.add("literal");
                return super.visitLiteralExpr(literalExpr);
            }
        }.accept(statement);

        assertEquals(List.of("merge", "table", "hint", "table", "top", "literal", "clause", "literal", "update", "clause", "delete", "clause", "doNothing", "clause", "insert"), visits.subList(0, 15));
    }
}
