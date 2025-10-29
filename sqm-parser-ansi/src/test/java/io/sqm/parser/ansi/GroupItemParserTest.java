package io.sqm.parser.ansi;

import io.sqm.core.ColumnExpr;
import io.sqm.core.Expression;
import io.sqm.core.GroupItem;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Objects;

class GroupItemParserTest {

    private final ParseContext ctx = ParseContext.of(new AnsiSpecs());
    private final GroupItemParser parser = new GroupItemParser();

    // ---------- Positional (GROUP BY 1, 2, ...) ----------

    @Test
    @DisplayName("Ordinal: '1' -> position=1")
    void ordinal_simple() {
        ParseResult<GroupItem> res = parser.parse("1", ctx);
        Assertions.assertTrue(res.ok(), () -> "expected ok, got error: " + res.errorMessage());
        Assertions.assertNull(res.value().expr(), "column must be null for positional group item");
        Assertions.assertEquals(1, res.value().ordinal());
    }

    @Test
    @DisplayName("Ordinal with spaces: '  2  ' -> position=2")
    void ordinal_trimmed() {
        ParseResult<GroupItem> res = parser.parse("  2  ", ctx);
        Assertions.assertTrue(res.ok());
        Assertions.assertNull(res.value().expr());
        Assertions.assertEquals(2, res.value().ordinal());
    }

    @Test
    @DisplayName("Ordinal zero: '0' -> error (must be positive)")
    void ordinal_zero_error() {
        ParseResult<GroupItem> res = parser.parse("0", ctx);
        Assertions.assertFalse(res.ok());
        Assertions.assertTrue(Objects.requireNonNull(res.errorMessage()).toLowerCase().contains("positive"), "error should mention positive integer");
    }

    // ---------- Column-based (GROUP BY t.c, "T"."Name", c) ----------

    @Test
    @DisplayName("Column: 'c' -> ColumnExpr.of(\"c\")")
    void column_simple() {
        ParseResult<GroupItem> res = parser.parse("c", ctx);
        Assertions.assertTrue(res.ok(), () -> "expected ok, got error: " + res.errorMessage());

        GroupItem gi = res.value();
        Assertions.assertNotNull(gi.expr(), "column must be set for column-based group item");
        Assertions.assertNull(gi.ordinal(), "position must be null for column-based group item");

        Expression expected = ColumnExpr.of("c");
        Assertions.assertEquals(expected, gi.expr());
    }

    @Test
    @DisplayName("Qualified: 't.c' -> Column.of(\"c\").from(\"t\")")
    void column_qualified() {
        ParseResult<GroupItem> res = parser.parse("t.c", ctx);
        Assertions.assertTrue(res.ok(), () -> "expected ok, got error: " + res.errorMessage());

        Expression expected = ColumnExpr.of("c").inTable("t");
        Assertions.assertEquals(expected, res.value().expr());
        Assertions.assertNull(res.value().ordinal());
    }

    @Test
    @DisplayName("Quoted qualified: '\"T\".\"Name\"' -> Column.of(\"Name\").from(\"T\")")
    void column_quotedQualified() {
        ParseResult<GroupItem> res = parser.parse("\"T\".\"Name\"", ctx);
        Assertions.assertTrue(res.ok(), () -> "expected ok, got error: " + res.errorMessage());

        Expression expected = ColumnExpr.of("Name").inTable("T");
        Assertions.assertEquals(expected, res.value().expr());
        Assertions.assertNull(res.value().ordinal());
    }

    // ---------- Errors / propagation ----------

    @Test
    @DisplayName("Blank -> error: Missing expr")
    void blank_error() {
        ParseResult<GroupItem> res = parser.parse("   ", ctx);
        Assertions.assertFalse(res.ok());
        Assertions.assertTrue(Objects.requireNonNull(res.errorMessage()).toLowerCase().contains("spec cannot be blank"), "should mention missing expr");
    }

    @Test
    @DisplayName("Garbage (e.g., '-') -> error propagated from column parser")
    void garbage_error() {
        ParseResult<GroupItem> res = parser.parse("-", ctx);
        Assertions.assertFalse(res.ok(), "dash is not a valid ordinal or column");
        Assertions.assertNotNull(res.errorMessage());
        Assertions.assertFalse(Objects.requireNonNull(res.errorMessage()).isBlank());
    }
}
