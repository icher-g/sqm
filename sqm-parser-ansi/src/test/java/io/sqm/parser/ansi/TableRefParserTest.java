package io.sqm.parser.ansi;

import io.sqm.core.QueryTable;
import io.sqm.core.Table;
import io.sqm.core.TableRef;
import io.sqm.parser.TableRefParser;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.IdentifierQuoting;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TableRefParserTest {

    private final ParseContext ctx = ParseContext.of(new AnsiSpecs());
    private final TableRefParser parser = new TableRefParser();
    private final IdentifierQuoting quoting = IdentifierQuoting.of('"');

    private ParseResult<? extends TableRef> parse(String sql) {
        return ctx.parse(parser, Cursor.of(sql, quoting));
    }

    @Test
    @DisplayName("Parses table only")
    void table_only() {
        var r = parse("products");
        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        Table t = (Table) r.value();
        Assertions.assertEquals("products", t.name());
        Assertions.assertNull(t.schema());
        Assertions.assertNull(t.alias());
    }

    @Test
    @DisplayName("Parses schema.table with bare alias")
    void schema_table_alias() {
        var r = parse("sales.products p");
        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        Table t = (Table) r.value();
        Assertions.assertEquals("sales", t.schema());
        Assertions.assertEquals("products", t.name());
        Assertions.assertEquals("p", t.alias());
    }

    @Test
    @DisplayName("Parses schema.table with AS alias")
    void schema_table_as_alias() {
        var r = parse("sales.products AS p");
        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        Table t = (Table) r.value();
        Assertions.assertEquals("sales", t.schema());
        Assertions.assertEquals("products", t.name());
        Assertions.assertEquals("p", t.alias());
    }

    @Test
    @DisplayName("Parses multi-part name (server.db.schema.table)")
    void multi_part_name() {
        var r = parse("srv.db.sales.products prod");
        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        Table t = (Table) r.value();
        Assertions.assertEquals("srv.db.sales", t.schema());
        Assertions.assertEquals("products", t.name());
        Assertions.assertEquals("prod", t.alias());
    }

    @Test
    @DisplayName("Errors on trailing dot")
    void error_trailing_dot() {
        var r = parse("sales.");
        Assertions.assertFalse(r.ok());
    }

    @Test
    @DisplayName("Errors when alias token missing after AS")
    void error_as_without_alias() {
        var r = parse("products AS");
        Assertions.assertFalse(r.ok());
    }

    @Test
    @DisplayName("Errors on extra tokens")
    void error_extra_tokens() {
        var r = parse("products p unexpected");
        Assertions.assertFalse(r.ok());
    }

    @Test
    @DisplayName("From a sub query")
    void select_from_subquery() {
        var r = parse("(SELECT * FROM t)");
        Assertions.assertTrue(r.ok());
        Assertions.assertInstanceOf(QueryTable.class, r.value());
    }
}
