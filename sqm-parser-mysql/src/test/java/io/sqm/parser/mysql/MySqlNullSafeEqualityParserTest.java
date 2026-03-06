package io.sqm.parser.mysql;

import io.sqm.core.ComparisonOperator;
import io.sqm.core.ComparisonPredicate;
import io.sqm.core.Predicate;
import io.sqm.parser.ansi.AnsiSpecs;
import io.sqm.parser.mysql.spi.MySqlSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MySqlNullSafeEqualityParserTest {

    @Test
    void parsesNullSafeEqualityOperator() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(Predicate.class, "a <=> b");

        assertTrue(result.ok());
        var predicate = assertInstanceOf(ComparisonPredicate.class, result.value());
        assertEquals(ComparisonOperator.NULL_SAFE_EQ, predicate.operator());
    }

    @Test
    void rejectsNullSafeEqualityForAnsiDialect() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(Predicate.class, "a <=> b");

        assertTrue(result.isError());
        assertTrue(result.problems().getFirst().message().toLowerCase().contains("null-safe equality"));
    }
}


