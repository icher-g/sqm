package io.cherlabs.sqm.parser.ansi;

import io.cherlabs.sqm.core.Column;
import io.cherlabs.sqm.core.Group;
import io.cherlabs.sqm.parser.ansi.statement.GroupParser;
import io.cherlabs.sqm.parser.spi.ParseContext;
import io.cherlabs.sqm.parser.spi.ParseResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GroupParserTest {

    private final ParseContext ctx = ParseContext.of(new AnsiSpecs());
    private final GroupParser parser = new GroupParser();

    // ---------- Positional (GROUP BY 1, 2, ...) ----------

    @Test
    @DisplayName("Ordinal: '1' -> position=1")
    void ordinal_simple() {
        ParseResult<Group> res = parser.parse("1", ctx);
        Assertions.assertTrue(res.ok(), () -> "expected ok, got error: " + res.errorMessage());
        Assertions.assertNull(res.value().column(), "column must be null for positional group item");
        Assertions.assertEquals(1, res.value().ordinal());
    }

    @Test
    @DisplayName("Ordinal with spaces: '  2  ' -> position=2")
    void ordinal_trimmed() {
        ParseResult<Group> res = parser.parse("  2  ", ctx);
        Assertions.assertTrue(res.ok());
        Assertions.assertNull(res.value().column());
        Assertions.assertEquals(2, res.value().ordinal());
    }

    @Test
    @DisplayName("Ordinal zero: '0' -> error (must be positive)")
    void ordinal_zero_error() {
        ParseResult<Group> res = parser.parse("0", ctx);
        Assertions.assertFalse(res.ok());
        Assertions.assertTrue(res.errorMessage().toLowerCase().contains("positive"), "error should mention positive integer");
    }

    // ---------- Column-based (GROUP BY t.c, "T"."Name", c) ----------

    @Test
    @DisplayName("Column: 'c' -> Column.of(\"c\")")
    void column_simple() {
        ParseResult<Group> res = parser.parse("c", ctx);
        Assertions.assertTrue(res.ok(), () -> "expected ok, got error: " + res.errorMessage());

        Group gi = res.value();
        Assertions.assertNotNull(gi.column(), "column must be set for column-based group item");
        Assertions.assertNull(gi.ordinal(), "position must be null for column-based group item");

        Column expected = Column.of("c");
        Assertions.assertEquals(expected, gi.column());
    }

    @Test
    @DisplayName("Qualified: 't.c' -> Column.of(\"c\").from(\"t\")")
    void column_qualified() {
        ParseResult<Group> res = parser.parse("t.c", ctx);
        Assertions.assertTrue(res.ok(), () -> "expected ok, got error: " + res.errorMessage());

        Column expected = Column.of("c").from("t");
        Assertions.assertEquals(expected, res.value().column());
        Assertions.assertNull(res.value().ordinal());
    }

    @Test
    @DisplayName("Quoted qualified: '\"T\".\"Name\"' -> Column.of(\"Name\").from(\"T\")")
    void column_quotedQualified() {
        ParseResult<Group> res = parser.parse("\"T\".\"Name\"", ctx);
        Assertions.assertTrue(res.ok(), () -> "expected ok, got error: " + res.errorMessage());

        Column expected = Column.of("Name").from("T");
        Assertions.assertEquals(expected, res.value().column());
        Assertions.assertNull(res.value().ordinal());
    }

    // ---------- Errors / propagation ----------

    @Test
    @DisplayName("Blank -> error: Missing expr")
    void blank_error() {
        ParseResult<Group> res = parser.parse("   ", ctx);
        Assertions.assertFalse(res.ok());
        Assertions.assertTrue(res.errorMessage().toLowerCase().contains("spec cannot be blank"), "should mention missing expr");
    }

    @Test
    @DisplayName("Garbage (e.g., '-') -> error propagated from column parser")
    void garbage_error() {
        ParseResult<Group> res = parser.parse("-", ctx);
        Assertions.assertFalse(res.ok(), "dash is not a valid ordinal or column");
        Assertions.assertNotNull(res.errorMessage());
        Assertions.assertFalse(res.errorMessage().isBlank());
    }
}
