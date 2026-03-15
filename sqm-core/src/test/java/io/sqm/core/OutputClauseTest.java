package io.sqm.core;

import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.deleted;
import static io.sqm.dsl.Dsl.id;
import static io.sqm.dsl.Dsl.inserted;
import static io.sqm.dsl.Dsl.output;
import static io.sqm.dsl.Dsl.outputInto;
import static io.sqm.dsl.Dsl.outputItem;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutputClauseTest {

    @Test
    void createsOutputClauseWithOptionalInto() {
        var clause = output(
            outputInto(tbl("audit"), id("new_id"), id("old_total")),
            outputItem(inserted("id")),
            outputItem(deleted("total"), "old_total"));

        assertEquals(2, clause.items().size());
        assertEquals("audit", clause.into().target().name().value());
        assertEquals(2, clause.into().columns().size());
        assertEquals(OutputRowSource.INSERTED, clause.items().getFirst().expression().matchExpression()
            .outputColumn(OutputColumnExpr::source)
            .orElse(null));
        assertEquals("old_total", clause.items().get(1).alias().value());
    }

    @Test
    void enforcesRequiredMembers() {
        assertThrows(IllegalArgumentException.class, () -> OutputClause.of(java.util.List.of()));
        assertThrows(NullPointerException.class, () -> OutputItem.of(null));
        assertThrows(NullPointerException.class, () -> OutputInto.of(null));
        assertThrows(NullPointerException.class, () -> OutputColumnExpr.of(null, id("x")));
        assertThrows(NullPointerException.class, () -> OutputColumnExpr.of(OutputRowSource.INSERTED, null));
    }

    @Test
    void copiesIntoColumnsImmutably() {
        var into = outputInto(tbl("audit"), id("col1"));

        assertTrue(into.columns().contains(id("col1")));
        assertThrows(UnsupportedOperationException.class, () -> into.columns().add(id("col2")));
    }
}
