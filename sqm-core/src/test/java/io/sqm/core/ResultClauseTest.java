package io.sqm.core;

import io.sqm.dsl.Dsl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class ResultClauseTest {

    @Test
    void createsOutputClauseWithOptionalInto() {
        var clause = result(
            resultRelationTarget(tbl("audit"), id("new_id"), id("old_total")),
            insertedAll(),
            inserted("id"),
            deleted("total").as("old_total"));

        assertEquals(3, clause.items().size());
        var target = assertInstanceOf(RelationResultTarget.class, clause.target());
        assertEquals(
            "audit",
            target.target().matchTableRef().table(table -> table.name().value()).orElseThrow(AssertionError::new)
        );
        assertEquals(2, target.columns().size());
        assertEquals(OutputRowSource.INSERTED, clause.items().getFirst().matchResultItem().outputStar(OutputStarResultItem::source).orElse(null));
        assertEquals(OutputRowSource.INSERTED, clause.items().get(1).matchResultItem().expr(e -> e.expr().matchExpression()
                .outputColumn(OutputColumnExpr::source)
                .orElse(null))
            .orElse(null));
        assertEquals("old_total", clause.items().get(2).matchResultItem().expr(e -> e.alias().value()).orElse(null));
    }

    @Test
    void enforcesRequiredMembers() {
        assertThrows(IllegalArgumentException.class, () -> ResultClause.of(java.util.List.of()));
        assertThrows(NullPointerException.class, () -> ResultItem.expr(null));
        assertThrows(NullPointerException.class, () -> RelationResultTarget.of(null));
        assertThrows(NullPointerException.class, () -> OutputColumnExpr.of(null, id("x")));
        assertThrows(NullPointerException.class, () -> OutputColumnExpr.of(OutputRowSource.INSERTED, null));
        assertThrows(NullPointerException.class, () -> OutputStarResultItem.of(null));
    }

    @Test
    void copiesIntoColumnsImmutably() {
        var into = Dsl.resultRelationTarget(tbl("audit"), id("col1"));

        assertTrue(into.columns().contains(id("col1")));
        assertThrows(UnsupportedOperationException.class, () -> into.columns().add(id("col2")));
    }

    @Test
    void supportsStringBasedDslRelationResultTargetOverloads() {
        var fromTable = Dsl.resultRelationTarget(tbl("audit"), "col_a", "col_b");
        var fromName = Dsl.resultRelationTarget("audit", "col_a", "col_b");

        assertEquals(List.of(id("col_a"), id("col_b")), fromTable.columns());
        assertEquals(
            "audit",
            fromName.target().matchTableRef().table(table -> table.name().value()).orElseThrow(AssertionError::new)
        );
        assertEquals(List.of(id("col_a"), id("col_b")), fromName.columns());
    }

    @Test
    void supportsNonTableRelationResultTargetTargetsInSharedModel() {
        var into = Dsl.resultRelationTarget(Dsl.tbl(select(lit(1L)).build()).as("audit_rows"), "col_a");

        assertEquals(
            "audit_rows",
            into.target().matchTableRef().query(queryTable -> queryTable.alias().value()).orElse(null)
        );
        assertEquals(List.of(id("col_a")), into.columns());
    }

    @Test
    void supportsVariableTableRelationResultTargetTargets() {
        var into = Dsl.resultRelationTarget(tableVar("@audit"), "col_a");

        assertEquals(
            "audit",
            into.target().matchTableRef().variableTable(variable -> variable.name().value()).orElseThrow(AssertionError::new)
        );
        assertEquals(List.of(id("col_a")), into.columns());
    }

    @Test
    void supportsVariableResultTargets() {
        var target = Dsl.resultVariableTarget(param("id"), param(1));
        var clause = Dsl.result(target, col("id"), col("name"));

        assertInstanceOf(VariableResultTarget.class, clause.target());
        assertSame(target, clause.target());
        assertEquals(2, target.variables().size());
    }

    @Test
    void rejectsQuotedVariableTableNames() {
        assertThrows(IllegalArgumentException.class, () -> tableVar(id("audit", QuoteStyle.DOUBLE_QUOTE)));
    }
}
