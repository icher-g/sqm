package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TableTest {

    @Test
    void of() {
        var table = Table.of("t");
        assertEquals("t", table.name());
        assertNull(table.schema());
        table = Table.of("dbo", "t");
        assertEquals("t", table.name());
        assertEquals("dbo", table.schema());
    }

    @Test
    void as() {
        var table = Table.of("t").as("a");
        assertEquals("t", table.name());
        assertEquals("a", table.alias());
    }

    @Test
    void inSchema() {
        var table = Table.of("t");
        assertEquals("t", table.name());
        assertNull(table.schema());
        table = table.inSchema("dbo");
        assertEquals("t", table.name());
        assertEquals("dbo", table.schema());
    }
}