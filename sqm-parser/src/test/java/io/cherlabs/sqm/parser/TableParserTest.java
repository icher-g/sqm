package io.cherlabs.sqm.parser;

import io.cherlabs.sqm.core.NamedTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TableParserTest {

    private final TableParser parser = new TableParser();

    @Test
    @DisplayName("Parses table only")
    void table_only() {
        var r = parser.parse("products");
        assertTrue(r.ok(), () -> "problems: " + r.problems());
        NamedTable t = (NamedTable) r.value();
        assertEquals("products", t.name());
        assertNull(t.schema());
        assertNull(t.alias());
    }

    @Test
    @DisplayName("Parses schema.table with bare alias")
    void schema_table_alias() {
        var r = parser.parse("sales.products p");
        assertTrue(r.ok(), () -> "problems: " + r.problems());
        NamedTable t = (NamedTable) r.value();
        assertEquals("sales", t.schema());
        assertEquals("products", t.name());
        assertEquals("p", t.alias());
    }

    @Test
    @DisplayName("Parses schema.table with AS alias")
    void schema_table_as_alias() {
        var r = parser.parse("sales.products AS p");
        assertTrue(r.ok(), () -> "problems: " + r.problems());
        NamedTable t = (NamedTable) r.value();
        assertEquals("sales", t.schema());
        assertEquals("products", t.name());
        assertEquals("p", t.alias());
    }

    @Test
    @DisplayName("Parses multi-part name (server.db.schema.table)")
    void multi_part_name() {
        var r = parser.parse("srv.db.sales.products prod");
        assertTrue(r.ok(), () -> "problems: " + r.problems());
        NamedTable t = (NamedTable) r.value();
        assertEquals("srv.db.sales", t.schema());
        assertEquals("products", t.name());
        assertEquals("prod", t.alias());
    }

    @Test
    @DisplayName("Errors on trailing dot")
    void error_trailing_dot() {
        var r = parser.parse("sales.");
        assertFalse(r.ok());
    }

    @Test
    @DisplayName("Errors when alias token missing after AS")
    void error_as_without_alias() {
        var r = parser.parse("products AS");
        assertFalse(r.ok());
    }

    @Test
    @DisplayName("Errors on extra tokens")
    void error_extra_tokens() {
        var r = parser.parse("products p unexpected");
        assertFalse(r.ok());
    }
}
