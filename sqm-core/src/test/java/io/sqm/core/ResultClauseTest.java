package io.sqm.core;

import io.sqm.dsl.Dsl;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class ResultClauseTest {

    @Test
    void createsOutputClauseWithOptionalInto() {
        var clause = result(
            resultInto(tbl("audit"), id("new_id"), id("old_total")),
            insertedAll(),
            inserted("id"),
            deleted("total").as("old_total"));

        assertEquals(3, clause.items().size());
        assertEquals("audit", clause.into().target().name().value());
        assertEquals(2, clause.into().columns().size());
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
        assertThrows(NullPointerException.class, () -> ResultInto.of(null));
        assertThrows(NullPointerException.class, () -> OutputColumnExpr.of(null, id("x")));
        assertThrows(NullPointerException.class, () -> OutputColumnExpr.of(OutputRowSource.INSERTED, null));
        assertThrows(NullPointerException.class, () -> OutputStarResultItem.of(null));
    }

    @Test
    void copiesIntoColumnsImmutably() {
        var into = Dsl.resultInto(tbl("audit"), id("col1"));

        assertTrue(into.columns().contains(id("col1")));
        assertThrows(UnsupportedOperationException.class, () -> into.columns().add(id("col2")));
    }
}
