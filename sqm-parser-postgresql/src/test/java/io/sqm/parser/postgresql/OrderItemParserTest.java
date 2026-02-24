package io.sqm.parser.postgresql;

import io.sqm.core.Direction;
import io.sqm.core.Nulls;
import io.sqm.core.OrderItem;
import io.sqm.core.QualifiedName;
import io.sqm.parser.ansi.OrderItemParser;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Objects;

class OrderItemParserTest {

    private final ParseContext ctx = ParseContext.of(new PostgresSpecs());
    private final OrderItemParser parser = new OrderItemParser();

    private ParseResult<? extends OrderItem> parse(String sql) {
        return ctx.parse(parser, sql);
    }

    @Nested
    @DisplayName("Happy path")
    class HappyPath {

        @Test
        @DisplayName("USING operator with expression")
        void using_operator_with_expression() {
            var res = parse("c USING < NULLS LAST");
            Assertions.assertTrue(res.ok(), () -> "unexpected error: " + res.errorMessage());
            OrderItem oi = res.value();
            Assertions.assertNotNull(oi.expr());
            Assertions.assertEquals("<", oi.usingOperator());
            Assertions.assertEquals(Nulls.LAST, oi.nulls());
            Assertions.assertNull(oi.direction());
        }

        @Test
        @DisplayName("USING operator with ordinal")
        void using_operator_with_ordinal() {
            var res = parse("1 USING >");
            Assertions.assertTrue(res.ok(), () -> "unexpected error: " + res.errorMessage());
            OrderItem oi = res.value();
            Assertions.assertEquals(1, oi.ordinal());
            Assertions.assertEquals(">", oi.usingOperator());
        }

        @Test
        @DisplayName("USING operator with question mark token")
        void using_operator_with_qmark() {
            var res = parse("c USING ?");
            Assertions.assertTrue(res.ok(), () -> "unexpected error: " + res.errorMessage());
            OrderItem oi = res.value();
            Assertions.assertEquals("?", oi.usingOperator());
        }

        @Test
        @DisplayName("Direction, nulls, and collate without USING")
        void direction_nulls_collate() {
            var res = parse("t.c COLLATE \"de-CH\" DESC NULLS FIRST");
            Assertions.assertTrue(res.ok(), () -> "unexpected error: " + res.errorMessage());
            OrderItem oi = res.value();
            Assertions.assertNotNull(oi.expr());
            Assertions.assertEquals(Direction.DESC, oi.direction());
            Assertions.assertEquals(Nulls.FIRST, oi.nulls());
            Assertions.assertEquals(QualifiedName.of(io.sqm.core.Identifier.of("de-CH", io.sqm.core.QuoteStyle.DOUBLE_QUOTE)), oi.collate());
        }

        @Test
        @DisplayName("Ordinal with direction")
        void ordinal_with_direction() {
            var res = parse("2 ASC");
            Assertions.assertTrue(res.ok(), () -> "unexpected error: " + res.errorMessage());
            OrderItem oi = res.value();
            Assertions.assertEquals(2, oi.ordinal());
            Assertions.assertEquals(Direction.ASC, oi.direction());
        }
    }

    @Nested
    @DisplayName("Error cases")
    class Errors {

        @Test
        @DisplayName("USING without operator -> error")
        void using_without_operator() {
            var res = parse("c USING");
            Assertions.assertFalse(res.ok());
            Assertions.assertTrue(Objects.requireNonNull(res.errorMessage()).contains("Expected operator after USING"));
        }

        @Test
        @DisplayName("USING with direction -> error")
        void using_with_direction() {
            var res = parse("c USING < DESC");
            Assertions.assertFalse(res.ok());
            Assertions.assertTrue(Objects.requireNonNull(res.errorMessage()).contains("USING operator cannot be combined with ASC/DESC"));
        }

        @Test
        @DisplayName("Direction then USING -> error")
        void direction_then_using() {
            var res = parse("c DESC USING <");
            Assertions.assertFalse(res.ok());
            Assertions.assertTrue(Objects.requireNonNull(res.errorMessage()).contains("USING operator cannot be combined with ASC/DESC"));
        }

        @Test
        @DisplayName("USING specified more than once -> error")
        void using_twice() {
            var res = parse("c USING < USING >");
            Assertions.assertFalse(res.ok());
            Assertions.assertTrue(Objects.requireNonNull(res.errorMessage()).contains("USING specified more than once"));
        }

        @Test
        @DisplayName("Duplicate direction -> error")
        void duplicate_direction() {
            var res = parse("c ASC DESC");
            Assertions.assertFalse(res.ok());
            Assertions.assertTrue(Objects.requireNonNull(res.errorMessage()).contains("Direction specified more than once"));
        }

        @Test
        @DisplayName("Duplicate NULLS -> error")
        void duplicate_nulls() {
            var res = parse("c NULLS FIRST NULLS LAST");
            Assertions.assertFalse(res.ok());
            Assertions.assertTrue(Objects.requireNonNull(res.errorMessage()).contains("NULLS specified more than once"));
        }

        @Test
        @DisplayName("Duplicate COLLATE -> error")
        void duplicate_collate() {
            var res = parse("c COLLATE de_CH COLLATE fr_CH");
            Assertions.assertFalse(res.ok());
            Assertions.assertTrue(Objects.requireNonNull(res.errorMessage()).contains("COLLATE specified more than once"));
        }

        @Test
        @DisplayName("NULLS without value -> error")
        void nulls_without_value() {
            var res = parse("c NULLS");
            Assertions.assertFalse(res.ok());
            Assertions.assertTrue(Objects.requireNonNull(res.errorMessage()).contains("Expected FIRST | LAST | DEFAULT"));
        }

        @Test
        @DisplayName("COLLATE without name -> error")
        void collate_without_name() {
            var res = parse("c COLLATE");
            Assertions.assertFalse(res.ok());
            Assertions.assertTrue(Objects.requireNonNull(res.errorMessage()).contains("Expected collation name after COLLATE"));
        }

        @Test
        @DisplayName("Invalid ordinal -> error")
        void invalid_ordinal() {
            var res = parse("0");
            Assertions.assertFalse(res.ok());
            Assertions.assertTrue(Objects.requireNonNull(res.errorMessage()).contains("positive integer"));
        }
    }
}

