package io.sqm.parser.ansi;

import io.sqm.core.QueryTable;
import io.sqm.core.QuoteStyle;
import io.sqm.core.SampledTable;
import io.sqm.core.Table;
import io.sqm.core.TablePartitionSpec;
import io.sqm.core.TableRef;
import io.sqm.core.TableVersionSpec;
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

    private ParseResult<? extends TableRef> parseWithAllFeatures(String sql) {
        var allFeatures = ParseContext.of(new TestSpecs());
        return allFeatures.parse(parser, Cursor.of(sql, allFeatures.identifierQuoting()));
    }

    @Test
    @DisplayName("Parses table only")
    void table_only() {
        var r = parse("products");
        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        Table t = (Table) r.value();
        Assertions.assertEquals("products", t.name().value());
        Assertions.assertNull(t.schema());
        Assertions.assertNull(t.alias());
    }

    @Test
    @DisplayName("Parses schema.table with bare alias")
    void schema_table_alias() {
        var r = parse("sales.products p");
        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        Table t = (Table) r.value();
        Assertions.assertEquals("sales", t.schema().value());
        Assertions.assertEquals("products", t.name().value());
        Assertions.assertEquals("p", t.alias().value());
    }

    @Test
    @DisplayName("Parses schema.table with AS alias")
    void schema_table_as_alias() {
        var r = parse("sales.products AS p");
        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        Table t = (Table) r.value();
        Assertions.assertEquals("sales", t.schema().value());
        Assertions.assertEquals("products", t.name().value());
        Assertions.assertEquals("p", t.alias().value());
    }

    @Test
    @DisplayName("Preserves quote metadata for table/schema/alias identifiers")
    void preserves_quote_metadata() {
        var r = parse("\"Sales\".\"Users\" AS \"U\"");
        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        Table t = (Table) r.value();
        Assertions.assertEquals("Sales", t.schema().value());
        Assertions.assertEquals("Users", t.name().value());
        Assertions.assertEquals("U", t.alias().value());
        Assertions.assertEquals(QuoteStyle.DOUBLE_QUOTE, t.schema().quoteStyle());
        Assertions.assertEquals(QuoteStyle.DOUBLE_QUOTE, t.name().quoteStyle());
        Assertions.assertEquals(QuoteStyle.DOUBLE_QUOTE, t.alias().quoteStyle());
    }

    @Test
    @DisplayName("Parses multi-part name (server.db.schema.table)")
    void multi_part_name() {
        var r = parse("srv.db.sales.products prod");
        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        Table t = (Table) r.value();
        Assertions.assertEquals("srv.db.sales", t.schema().value());
        Assertions.assertEquals("products", t.name().value());
        Assertions.assertEquals("prod", t.alias().value());
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
    @DisplayName("Rejects ONLY table in ANSI")
    void error_only_table() {
        var r = parse("ONLY products");
        Assertions.assertFalse(r.ok());
    }

    @Test
    @DisplayName("Rejects table inheritance star in ANSI")
    void error_table_inheritance_star() {
        var r = parse("products *");
        Assertions.assertFalse(r.ok());
    }

    @Test
    @DisplayName("Rejects inheritance star with schema")
    void error_table_inheritance_star_with_schema() {
        var r = parse("sales.products *");
        Assertions.assertFalse(r.ok());
    }

    @Test
    @DisplayName("Rejects table versioning in ANSI")
    void error_table_versioning() {
        var r = parse("products AS OF SCN 42");
        Assertions.assertFalse(r.ok());
    }

    @Test
    @DisplayName("Rejects table partition spec in ANSI")
    void error_table_partition_spec() {
        var r = parse("products PARTITION (p0)");
        Assertions.assertFalse(r.ok());
    }

    @Test
    @DisplayName("Rejects table sample in ANSI")
    void error_table_sample() {
        var r = parse("products TABLESAMPLE SYSTEM (10)");
        Assertions.assertFalse(r.ok());
    }

    @Test
    @DisplayName("Parses table version, partition, and alias when feature gates allow them")
    void parses_table_access_modifiers_with_enabled_features() {
        var r = parseWithAllFeatures("orders AS OF SCN 123 PARTITION (p0, p1) o");

        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        var t = Assertions.assertInstanceOf(Table.class, r.value());
        Assertions.assertEquals("o", t.alias().value());
        Assertions.assertEquals(TableVersionSpec.TableVersionKind.AS_OF_SCN, t.version().kind());
        Assertions.assertEquals(TablePartitionSpec.TablePartitionSpecKind.PARTITION, t.partitionSpec().kind());
        Assertions.assertEquals("p0", t.partitionSpec().names().getFirst().value());
        Assertions.assertEquals("p1", t.partitionSpec().names().get(1).value());
    }

    @Test
    @DisplayName("Parses SQL Server style table version selectors through ANSI table parser hooks")
    void parses_system_time_version_selectors_with_enabled_features() {
        var fromTo = Assertions.assertInstanceOf(Table.class,
            parseWithAllFeatures("orders FOR SYSTEM_TIME FROM 10 TO 20").value());
        var between = Assertions.assertInstanceOf(Table.class,
            parseWithAllFeatures("orders FOR SYSTEM_TIME BETWEEN 10 AND 20").value());
        var contained = Assertions.assertInstanceOf(Table.class,
            parseWithAllFeatures("orders FOR SYSTEM_TIME CONTAINED IN (10, 20)").value());
        var all = Assertions.assertInstanceOf(Table.class,
            parseWithAllFeatures("orders FOR SYSTEM_TIME ALL").value());

        Assertions.assertEquals(TableVersionSpec.TableVersionKind.FROM_TO, fromTo.version().kind());
        Assertions.assertEquals(TableVersionSpec.TableVersionKind.BETWEEN, between.version().kind());
        Assertions.assertEquals(TableVersionSpec.TableVersionKind.CONTAINED_IN, contained.version().kind());
        Assertions.assertEquals(TableVersionSpec.TableVersionKind.ALL, all.version().kind());
    }

    @Test
    @DisplayName("Parses TABLESAMPLE as a relation transform when feature gates allow it")
    void parses_table_sample_transform_with_enabled_features() {
        var r = parseWithAllFeatures("products TABLESAMPLE BERNOULLI (10 ROWS) sampled_products");

        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        var sampled = Assertions.assertInstanceOf(SampledTable.class, r.value());
        Assertions.assertEquals("sampled_products", sampled.alias().value());
        Assertions.assertEquals("products", Assertions.assertInstanceOf(Table.class, sampled.source()).name().value());
    }

    @Test
    @DisplayName("Reports malformed table access modifier syntax")
    void errors_on_malformed_table_access_modifiers() {
        Assertions.assertFalse(parseWithAllFeatures("orders AS OF").ok());
        Assertions.assertFalse(parseWithAllFeatures("orders FOR SYSTEM_TIME").ok());
        Assertions.assertFalse(parseWithAllFeatures("orders PARTITION ()").ok());
        Assertions.assertFalse(parseWithAllFeatures("orders SUBPARTITION (sp0").ok());
    }

    @Test
    @DisplayName("From a sub query")
    void select_from_subquery() {
        var r = parse("(SELECT * FROM t)");
        Assertions.assertTrue(r.ok());
        Assertions.assertInstanceOf(QueryTable.class, r.value());
    }
}
