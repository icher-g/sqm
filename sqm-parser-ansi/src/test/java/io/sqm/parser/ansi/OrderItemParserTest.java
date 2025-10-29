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
    private final OrderItemParser parser = new OrderItemParser();

    @Nested
    @DisplayName("Happy path")
    class HappyPath {

        @Test
        @DisplayName("Bare column -> defaults (no dir, no nulls, no collate)")
        void bareColumn_defaults() {
            ParseResult<OrderItem> res = parser.parse("c", ctx);
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
            ParseResult<OrderItem> res = parser.parse("t.c DESC", ctx);
            Assertions.assertTrue(res.ok(), () -> "unexpected error: " + res.errorMessage());
            OrderItem oi = res.value();
            Assertions.assertNotNull(oi.expr());         // Column is parsed by ColumnSpecParser
            Assertions.assertEquals(Direction.DESC, oi.direction());
            Assertions.assertNull(oi.nulls());
            Assertions.assertNull(oi.collate());
        }

        @Test
        @DisplayName("Function expr + ASC + NULLS LAST (case-insensitive)")
        void function_with_dir_and_nulls() {
            ParseResult<OrderItem> res = parser.parse("lower(t.c) aSc nUlLs lAsT", ctx);
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
            ParseResult<OrderItem> res = parser.parse("name COLLATE de_CH", ctx);
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
            ParseResult<OrderItem> res = parser.parse("name COLLATE \"de-CH-x-phonebk\"", ctx);
            Assertions.assertTrue(res.ok(), () -> "unexpected error: " + res.errorMessage());
            OrderItem oi = res.value();
            Assertions.assertEquals("de-CH-x-phonebk", oi.collate());
        }

        @Test
        @DisplayName("All modifiers in any order")
        void all_modifiers_any_order() {
            ParseResult<OrderItem> res = parser.parse("LOWER(\"T\".\"Name\") NULLS FIRST COLLATE \"de-CH\" DESC", ctx);
            Assertions.assertTrue(res.ok(), () -> "unexpected error: " + res.errorMessage());
            OrderItem oi = res.value();
            Assertions.assertNotNull(oi.expr());
            Assertions.assertEquals(Direction.DESC, oi.direction());
            Assertions.assertEquals(Nulls.FIRST, oi.nulls());
            Assertions.assertEquals("de-CH", oi.collate());
        }
    }

    @Nested
    @DisplayName("Error cases")
    class Errors {

        @Test
        @DisplayName("Empty input -> Missing column")
        void empty_missing_column() {
            ParseResult<OrderItem> res = parser.parse("  ", ctx);
            Assertions.assertFalse(res.ok());
            Assertions.assertEquals("The spec cannot be blank.", res.errorMessage());
        }

        @Test
        @DisplayName("NULLS without value -> error")
        void nulls_without_value() {
            ParseResult<OrderItem> res = parser.parse("c NULLS", ctx);
            Assertions.assertFalse(res.ok());
            Assertions.assertEquals("Expected FIRST | LAST | DEFAULT after NULLS at 7", res.errorMessage());
        }

        @Test
        @DisplayName("NULLS invalid value -> error")
        void nulls_invalid_value() {
            ParseResult<OrderItem> res = parser.parse("c NULLS MAYBE", ctx);
            Assertions.assertFalse(res.ok());
            Assertions.assertEquals("Expected FIRST | LAST | DEFAULT after NULLS at 8", res.errorMessage());
        }

        @Test
        @DisplayName("Duplicate direction -> error")
        void duplicate_direction() {
            ParseResult<OrderItem> res = parser.parse("c ASC DESC", ctx);
            Assertions.assertFalse(res.ok());
            Assertions.assertEquals("Direction specified more than once at 3", res.errorMessage());
        }

        @Test
        @DisplayName("Duplicate NULLS -> error")
        void duplicate_nulls() {
            ParseResult<OrderItem> res = parser.parse("c NULLS FIRST NULLS LAST", ctx);
            Assertions.assertFalse(res.ok());
            Assertions.assertEquals("NULLS specified more than once at 4", res.errorMessage());
        }

        @Test
        @DisplayName("Duplicate COLLATE -> error")
        void duplicate_collate() {
            ParseResult<OrderItem> res = parser.parse("c COLLATE de_CH COLLATE fr_CH", ctx);
            Assertions.assertFalse(res.ok());
            Assertions.assertEquals("COLLATE specified more than once at 4", res.errorMessage());
        }

        @Test
        @DisplayName("COLLATE without name -> error")
        void collate_without_name() {
            ParseResult<OrderItem> res = parser.parse("c COLLATE", ctx);
            Assertions.assertFalse(res.ok());
            Assertions.assertEquals("Expected collation name after COLLATE at 9", res.errorMessage());
        }

        @Test
        @DisplayName("Unexpected trailing token -> error")
        void unexpected_trailing_token() {
            ParseResult<OrderItem> res = parser.parse("c ASC EXTRA", ctx);
            Assertions.assertFalse(res.ok());
            Assertions.assertTrue(Objects.requireNonNull(res.errorMessage()).startsWith("Unexpected token in ORDER BY item"),
                "should mention unexpected token");
        }
    }
}
