package io.cherlabs.sqlmodel.core.views;

import io.cherlabs.sqlmodel.core.*;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static io.cherlabs.sqlmodel.dsl.DSL.q;
import static org.junit.jupiter.api.Assertions.*;

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
        Query<?> q = q();
        QueryTable t = new QueryTable(q, "qt");
        assertTrue(Tables.name(t).isEmpty());
        assertEquals(Optional.of("qt"), Tables.alias(t));
        assertTrue(Tables.schema(t).isEmpty());
        assertEquals(Optional.of(q), Tables.query(t));
    }
}