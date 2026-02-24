package io.sqm.core;

import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.*;

class TableTest {

    @Test
    void of() {
        var table = tbl("t");
        assertEquals("t", table.name().value());
        assertNull(table.schema());
        assertEquals(Table.Inheritance.DEFAULT, table.inheritance());
        table = tbl("dbo", "t");
        assertEquals("t", table.name().value());
        assertEquals("dbo", table.schema().value());
        assertEquals(Table.Inheritance.DEFAULT, table.inheritance());
    }

    @Test
    void as() {
        var table = tbl("t").as("a");
        assertEquals("t", table.name().value());
        assertEquals("a", table.alias().value());
    }

    @Test
    void inSchema() {
        var table = tbl("t");
        assertEquals("t", table.name().value());
        assertNull(table.schema());
        table = table.inSchema("dbo");
        assertEquals("t", table.name().value());
        assertEquals("dbo", table.schema().value());
    }

    @Test
    void inheritance_flags() {
        var table = tbl("t").only();
        assertEquals(Table.Inheritance.ONLY, table.inheritance());
        table = table.includingDescendants();
        assertEquals(Table.Inheritance.INCLUDE_DESCENDANTS, table.inheritance());
    }

    @Test
    void inheritance_null_defaults() {
        var table = Table.of(Identifier.of("dbo"), Identifier.of("t"), Identifier.of("a"), null);
        assertEquals(Table.Inheritance.DEFAULT, table.inheritance());
    }

    @Test
    void preserves_identifier_quote_metadata() {
        var table = Table.of(
            Identifier.of("MySchema", QuoteStyle.DOUBLE_QUOTE),
            Identifier.of("User", QuoteStyle.DOUBLE_QUOTE),
            Identifier.of("u", QuoteStyle.BACKTICK),
            Table.Inheritance.DEFAULT
        );

        assertEquals("MySchema", table.schema().value());
        assertEquals("User", table.name().value());
        assertEquals("u", table.alias().value());
        assertEquals(QuoteStyle.DOUBLE_QUOTE, table.schema().quoteStyle());
        assertEquals(QuoteStyle.DOUBLE_QUOTE, table.name().quoteStyle());
        assertEquals(QuoteStyle.BACKTICK, table.alias().quoteStyle());
    }
}
