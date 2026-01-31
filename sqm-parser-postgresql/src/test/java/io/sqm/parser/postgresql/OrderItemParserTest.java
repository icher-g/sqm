package io.sqm.parser.postgresql;

import io.sqm.core.Nulls;
import io.sqm.core.OrderItem;
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
    }
}
