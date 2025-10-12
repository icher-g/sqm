package io.cherlabs.sqm.core.views;

import io.cherlabs.sqm.core.NamedTable;
import io.cherlabs.sqm.core.QueryTable;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static io.cherlabs.sqm.dsl.Dsl.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewsTablesTest {

    @Test
    void named_table_fields_are_exposed() {
        NamedTable t = new NamedTable("products", "p", "sales");
        assertEquals(Optional.of("products"), Tables.name(t));
        assertEquals(Optional.of("p"), Tables.alias(t));
        assertEquals(Optional.of("sales"), Tables.schema(t));
        assertTrue(Tables.query(t).isEmpty());
    }

    @Test
    void query_table_fields_are_exposed() {
        var q = query();
        QueryTable t = new QueryTable(q, "qt");
        assertTrue(Tables.name(t).isEmpty());
        assertEquals(Optional.of("qt"), Tables.alias(t));
        assertTrue(Tables.schema(t).isEmpty());
        assertEquals(Optional.of(q), Tables.query(t));
    }
}