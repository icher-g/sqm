package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NamedTableTest {

    @Test
    void as_sets_alias() {
        NamedTable t = new NamedTable("products", null, "sales");
        NamedTable a = t.as("p");
        assertEquals("products", a.name());
        assertEquals("p", a.alias());
        assertEquals("sales", a.schema());
        assertNotSame(t, a);
    }

    @Test
    void from_sets_schema() {
        NamedTable t = new NamedTable("products", "p", null);
        NamedTable s = t.from("sales");
        assertEquals("sales", s.schema());
        assertEquals("p", s.alias());
        assertEquals("products", s.name());
    }
}