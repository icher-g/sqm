package io.sqm.parser.postgresql;

import io.sqm.core.Table;
import io.sqm.core.TableRef;
import io.sqm.parser.TableRefParser;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.IdentifierQuoting;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TableRefParserTest {

    private final ParseContext ctx = ParseContext.of(new PostgresSpecs());
    private final TableRefParser parser = new TableRefParser();
    private final IdentifierQuoting quoting = IdentifierQuoting.of('"');

    private ParseResult<? extends TableRef> parse(String sql) {
        return ctx.parse(parser, Cursor.of(sql, quoting));
    }

    @Test
    @DisplayName("Parses ONLY table")
    void parses_only_table() {
        var r = parse("ONLY products");
        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        Table t = (Table) r.value();
        Assertions.assertEquals(Table.Inheritance.ONLY, t.inheritance());
        Assertions.assertEquals("products", t.name());
    }

    @Test
    @DisplayName("Parses table inheritance star")
    void parses_table_inheritance_star() {
        var r = parse("products *");
        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        Table t = (Table) r.value();
        Assertions.assertEquals(Table.Inheritance.INCLUDE_DESCENDANTS, t.inheritance());
        Assertions.assertEquals("products", t.name());
    }

    @Test
    @DisplayName("Parses schema table with inheritance star")
    void parses_schema_table_inheritance_star() {
        var r = parse("sales.products *");
        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        Table t = (Table) r.value();
        Assertions.assertEquals(Table.Inheritance.INCLUDE_DESCENDANTS, t.inheritance());
        Assertions.assertEquals("sales", t.schema());
        Assertions.assertEquals("products", t.name());
    }

    @Test
    @DisplayName("Parses table inheritance star with alias")
    void parses_table_star_with_alias() {
        var r = parse("products * AS p");
        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        Table t = (Table) r.value();
        Assertions.assertEquals(Table.Inheritance.INCLUDE_DESCENDANTS, t.inheritance());
        Assertions.assertEquals("p", t.alias());
    }

    @Test
    @DisplayName("Parses ONLY with alias")
    void parses_only_with_alias() {
        var r = parse("ONLY products p");
        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        Table t = (Table) r.value();
        Assertions.assertEquals(Table.Inheritance.ONLY, t.inheritance());
        Assertions.assertEquals("p", t.alias());
    }

    @Test
    @DisplayName("Rejects ONLY with inheritance star")
    void rejects_only_with_star() {
        var r = parse("ONLY products *");
        Assertions.assertFalse(r.ok());
    }

    @Test
    @DisplayName("Rejects ONLY without table name")
    void rejects_only_without_name() {
        var r = parse("ONLY");
        Assertions.assertFalse(r.ok());
    }
}
