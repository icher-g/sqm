package io.cherlabs.sqm.parser;

import io.cherlabs.sqm.core.Direction;
import io.cherlabs.sqm.core.Nulls;
import io.cherlabs.sqm.core.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderParserTest {

    private final OrderParser parser = new OrderParser();

    @Nested
    @DisplayName("Happy path")
    class HappyPath {

        @Test
        @DisplayName("Bare column -> defaults (no dir, no nulls, no collate)")
        void bareColumn_defaults() {
            ParseResult<Order> res = parser.parse("c");
            assertTrue(res.ok(), () -> "unexpected error: " + res.errorMessage());
            Order oi = res.value();
            assertNotNull(oi.column());
            assertNull(oi.direction());
            assertNull(oi.nulls());
            assertNull(oi.collate());
        }

        @Test
        @DisplayName("Qualified column + DESC")
        void qualified_with_desc() {
            ParseResult<Order> res = parser.parse("t.c DESC");
            assertTrue(res.ok(), () -> "unexpected error: " + res.errorMessage());
            Order oi = res.value();
            assertNotNull(oi.column());         // Column is parsed by ColumnSpecParser
            assertEquals(Direction.Desc, oi.direction());
            assertNull(oi.nulls());
            assertNull(oi.collate());
        }

        @Test
        @DisplayName("Function expr + ASC + NULLS LAST (case-insensitive)")
        void function_with_dir_and_nulls() {
            ParseResult<Order> res = parser.parse("lower(t.c) aSc nUlLs lAsT");
            assertTrue(res.ok(), () -> "unexpected error: " + res.errorMessage());
            Order oi = res.value();
            assertNotNull(oi.column());         // should be FunctionColumn under the hood
            assertEquals(Direction.Asc, oi.direction());
            assertEquals(Nulls.Last, oi.nulls());
            assertNull(oi.collate());
        }

        @Test
        @DisplayName("COLLATE bare identifier")
        void collate_bare() {
            ParseResult<Order> res = parser.parse("name COLLATE de_CH");
            assertTrue(res.ok(), () -> "unexpected error: " + res.errorMessage());
            Order oi = res.value();
            assertNotNull(oi.column());
            assertNull(oi.direction());
            assertNull(oi.nulls());
            assertEquals("de_CH", oi.collate());
        }

        @Test
        @DisplayName("COLLATE quoted identifier")
        void collate_quoted() {
            ParseResult<Order> res = parser.parse("name COLLATE \"de-CH-x-phonebk\"");
            assertTrue(res.ok(), () -> "unexpected error: " + res.errorMessage());
            Order oi = res.value();
            assertEquals("de-CH-x-phonebk", oi.collate());
        }

        @Test
        @DisplayName("All modifiers in any order")
        void all_modifiers_any_order() {
            ParseResult<Order> res = parser.parse("LOWER(\"T\".\"Name\") NULLS FIRST COLLATE \"de-CH\" DESC");
            assertTrue(res.ok(), () -> "unexpected error: " + res.errorMessage());
            Order oi = res.value();
            assertNotNull(oi.column());
            assertEquals(Direction.Desc, oi.direction());
            assertEquals(Nulls.First, oi.nulls());
            assertEquals("de-CH", oi.collate());
        }
    }

    @Nested
    @DisplayName("Error cases")
    class Errors {

        @Test
        @DisplayName("Empty input -> Missing column")
        void empty_missing_column() {
            ParseResult<Order> res = parser.parse("  ");
            assertFalse(res.ok());
            assertEquals("The spec cannot be blank.", res.errorMessage());
        }

        @Test
        @DisplayName("NULLS without value -> error")
        void nulls_without_value() {
            ParseResult<Order> res = parser.parse("c NULLS");
            assertFalse(res.ok());
            assertEquals("Expected FIRST | LAST | DEFAULT after NULLS at 7", res.errorMessage());
        }

        @Test
        @DisplayName("NULLS invalid value -> error")
        void nulls_invalid_value() {
            ParseResult<Order> res = parser.parse("c NULLS MAYBE");
            assertFalse(res.ok());
            assertEquals("Expected FIRST | LAST | DEFAULT after NULLS at 8", res.errorMessage());
        }

        @Test
        @DisplayName("Duplicate direction -> error")
        void duplicate_direction() {
            ParseResult<Order> res = parser.parse("c ASC DESC");
            assertFalse(res.ok());
            assertEquals("Direction specified more than once at 3", res.errorMessage());
        }

        @Test
        @DisplayName("Duplicate NULLS -> error")
        void duplicate_nulls() {
            ParseResult<Order> res = parser.parse("c NULLS FIRST NULLS LAST");
            assertFalse(res.ok());
            assertEquals("NULLS specified more than once at 4", res.errorMessage());
        }

        @Test
        @DisplayName("Duplicate COLLATE -> error")
        void duplicate_collate() {
            ParseResult<Order> res = parser.parse("c COLLATE de_CH COLLATE fr_CH");
            assertFalse(res.ok());
            assertEquals("COLLATE specified more than once at 4", res.errorMessage());
        }

        @Test
        @DisplayName("COLLATE without name -> error")
        void collate_without_name() {
            ParseResult<Order> res = parser.parse("c COLLATE");
            assertFalse(res.ok());
            assertEquals("Expected collation name after COLLATE at 9", res.errorMessage());
        }

        @Test
        @DisplayName("Unexpected trailing token -> error")
        void unexpected_trailing_token() {
            ParseResult<Order> res = parser.parse("c ASC EXTRA");
            assertFalse(res.ok());
            assertTrue(res.errorMessage().startsWith("Unexpected token in ORDER BY item"),
                    "should mention unexpected token");
        }
    }
}
