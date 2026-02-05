package io.sqm.parser.ansi;

import io.sqm.core.Direction;
import io.sqm.core.Nulls;
import io.sqm.core.OrderItem;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Objects;

class OrderItemParserTest {

    private final ParseContext ctx = ParseContext.of(new AnsiSpecs());
    private final ParseContext testCtx = ParseContext.of(new TestSpecs());
    private final OrderItemParser parser = new OrderItemParser();

    private ParseResult<? extends OrderItem> parse(String sql) {
        return ctx.parse(parser, sql);
    }

    private ParseResult<? extends OrderItem> parseWithTestSpecs(String sql) {
        return testCtx.parse(parser, sql);
    }

    @Nested
    @DisplayName("Happy path")
    class HappyPath {

        @Test
        @DisplayName("Bare column -> defaults (no dir, no nulls, no collate)")
        void bareColumn_defaults() {
            var res = parse("c");
            Assertions.assertTrue(res.ok(), () -> "unexpected error: " + res.errorMessage());
            OrderItem oi = res.value();
            Assertions.assertNotNull(oi.expr());
            Assertions.assertNull(oi.direction());
            Assertions.assertNull(oi.nulls());
            Assertions.assertNull(oi.collate());
        }

        @Test
        @DisplayName("Qualified column + DESC")
        void qualified_with_desc() {
            var res = parse("t.c DESC");
            Assertions.assertTrue(res.ok(), () -> "unexpected error: " + res.errorMessage());
            OrderItem oi = res.value();
            Assertions.assertNotNull(oi.expr());         // Column is parsed by ColumnSpecParser
            Assertions.assertEquals(Direction.DESC, oi.direction());
            Assertions.assertNull(oi.nulls());
            Assertions.assertNull(oi.collate());
        }

        @Test
        @DisplayName("Ordinal order item")
        void ordinal_item() {
            var res = parse("1 ASC");
            Assertions.assertTrue(res.ok(), () -> "unexpected error: " + res.errorMessage());
            OrderItem oi = res.value();
            Assertions.assertEquals(1, oi.ordinal());
            Assertions.assertEquals(Direction.ASC, oi.direction());
        }

        @Test
        @DisplayName("Function expr + ASC + NULLS LAST (case-insensitive)")
        void function_with_dir_and_nulls() {
            var res = parse("lower(t.c) aSc nUlLs lAsT");
            Assertions.assertTrue(res.ok(), () -> "unexpected error: " + res.errorMessage());
            OrderItem oi = res.value();
            Assertions.assertNotNull(oi.expr());         // should be FunctionColumn under the hood
            Assertions.assertEquals(Direction.ASC, oi.direction());
            Assertions.assertEquals(Nulls.LAST, oi.nulls());
            Assertions.assertNull(oi.collate());
        }

        @Test
        @DisplayName("COLLATE bare identifier")
        void collate_bare() {
            var res = parse("name COLLATE de_CH");
            Assertions.assertTrue(res.ok(), () -> "unexpected error: " + res.errorMessage());
            OrderItem oi = res.value();
            Assertions.assertNotNull(oi.expr());
            Assertions.assertNull(oi.direction());
            Assertions.assertNull(oi.nulls());
            Assertions.assertEquals("de_CH", oi.collate());
        }

        @Test
        @DisplayName("COLLATE quoted identifier")
        void collate_quoted() {
            var res = parse("name COLLATE \"de-CH-x-phonebk\"");
            Assertions.assertTrue(res.ok(), () -> "unexpected error: " + res.errorMessage());
            OrderItem oi = res.value();
            Assertions.assertEquals("de-CH-x-phonebk", oi.collate());
        }

        @Test
        @DisplayName("All modifiers in any order")
        void all_modifiers_any_order() {
            var res = parse("LOWER(\"T\".\"Name\") NULLS FIRST COLLATE \"de-CH\" DESC");
            Assertions.assertTrue(res.ok(), () -> "unexpected error: " + res.errorMessage());
            OrderItem oi = res.value();
            Assertions.assertNotNull(oi.expr());
            Assertions.assertEquals(Direction.DESC, oi.direction());
            Assertions.assertEquals(Nulls.FIRST, oi.nulls());
            Assertions.assertEquals("de-CH", oi.collate());
        }

        @Test
        @DisplayName("Expression-level COLLATE normalized into OrderItem")
        void expression_collate_normalized() {
            var res = parseWithTestSpecs("name COLLATE de_CH DESC");
            Assertions.assertTrue(res.ok(), () -> "unexpected error: " + res.errorMessage());
            OrderItem oi = res.value();
            Assertions.assertNotNull(oi.expr());
            Assertions.assertEquals(Direction.DESC, oi.direction());
            Assertions.assertEquals("de_CH", oi.collate());
        }
    }

    @Nested
    @DisplayName("Error cases")
    class Errors {

        @Test
        @DisplayName("Empty input -> Missing column")
        void empty_missing_column() {
            var res = parse("  ");
            Assertions.assertFalse(res.ok());
            Assertions.assertEquals("The spec cannot be blank.", res.errorMessage());
        }

        @Test
        @DisplayName("NULLS without value -> error")
        void nulls_without_value() {
            var res = parse("c NULLS");
            Assertions.assertFalse(res.ok());
            Assertions.assertEquals("Expected FIRST | LAST | DEFAULT after NULLS at 7", res.errorMessage());
        }

        @Test
        @DisplayName("NULLS invalid value -> error")
        void nulls_invalid_value() {
            var res = parse("c NULLS MAYBE");
            Assertions.assertFalse(res.ok());
            Assertions.assertEquals("Expected FIRST | LAST | DEFAULT after NULLS at 8", res.errorMessage());
        }

        @Test
        @DisplayName("Duplicate direction -> error")
        void duplicate_direction() {
            var res = parse("c ASC DESC");
            Assertions.assertFalse(res.ok());
            Assertions.assertEquals("Direction specified more than once at 3", res.errorMessage());
        }

        @Test
        @DisplayName("Duplicate NULLS -> error")
        void duplicate_nulls() {
            var res = parse("c NULLS FIRST NULLS LAST");
            Assertions.assertFalse(res.ok());
            Assertions.assertEquals("NULLS specified more than once at 4", res.errorMessage());
        }

        @Test
        @DisplayName("Duplicate COLLATE -> error")
        void duplicate_collate() {
            var res = parse("c COLLATE de_CH COLLATE fr_CH");
            Assertions.assertFalse(res.ok());
            Assertions.assertEquals("COLLATE specified more than once at 4", res.errorMessage());
        }

        @Test
        @DisplayName("COLLATE without name -> error")
        void collate_without_name() {
            var res = parse("c COLLATE");
            Assertions.assertFalse(res.ok());
            Assertions.assertEquals("Expected collation name after COLLATE at 9", res.errorMessage());
        }

        @Test
        @DisplayName("USING operator not supported in ANSI")
        void using_not_supported() {
            var res = parse("c USING <");
            Assertions.assertFalse(res.ok());
            Assertions.assertTrue(Objects.requireNonNull(res.errorMessage()).contains("ORDER BY ... USING is not supported"));
        }
        @Test
        @DisplayName("Unexpected trailing token -> error")
        void unexpected_trailing_token() {
            var res = parse("c ASC EXTRA");
            Assertions.assertFalse(res.ok());
            Assertions.assertTrue(Objects.requireNonNull(res.errorMessage()).startsWith("Expected EOF but found: EXTRA"),
                "should mention unexpected token");
        }
    }
}
