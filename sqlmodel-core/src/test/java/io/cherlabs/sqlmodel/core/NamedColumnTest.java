package io.cherlabs.sqlmodel.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NamedColumnTest {

    @Test
    void as_creates_new_with_alias() {
        NamedColumn c = new NamedColumn("id", null, "p");
        NamedColumn a = c.as("pid");
        assertEquals("id", a.name());
        assertEquals("pid", a.alias());
        assertEquals("p", a.table());
        assertNotSame(c, a);
    }

    @Test
    void from_sets_table() {
        NamedColumn c = new NamedColumn("id", null, null);
        NamedColumn t = c.from("p");
        assertEquals("p", t.table());
        assertEquals("id", t.name());
        assertNull(t.alias());
    }
}