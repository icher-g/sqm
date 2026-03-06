package io.sqm.parser.ansi;

import io.sqm.core.ComparisonOperator;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ComparisonOperatorParserTest {

    private final ComparisonOperatorParser parser = new ComparisonOperatorParser();

    @Test
    void parsesAllStandardOperators() {
        assertEquals(ComparisonOperator.EQ, parse("="));
        assertEquals(ComparisonOperator.NE, parse("<>"));
        assertEquals(ComparisonOperator.NE, parse("!="));
        assertEquals(ComparisonOperator.GT, parse(">"));
        assertEquals(ComparisonOperator.GTE, parse(">="));
        assertEquals(ComparisonOperator.LT, parse("<"));
        assertEquals(ComparisonOperator.LTE, parse("<="));
    }

    @Test
    void parsesNullSafeEqualityWhenFeatureSupported() {
        var ctx = ParseContext.of(new TestSpecs());
        var cur = Cursor.of("<=>", ctx.identifierQuoting());

        assertEquals(ComparisonOperator.NULL_SAFE_EQ, parser.parse(cur, ctx));
    }

    @Test
    void rejectsNullSafeEqualityWhenFeatureUnsupported() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var cur = Cursor.of("<=>", ctx.identifierQuoting());

        assertThrows(UnsupportedOperationException.class, () -> parser.parse(cur, ctx));
    }

    @Test
    void rejectsUnknownOperator() {
        var ctx = ParseContext.of(new TestSpecs());
        var cur = Cursor.of("&&", ctx.identifierQuoting());

        assertThrows(UnsupportedOperationException.class, () -> parser.parse(cur, ctx));
    }

    private ComparisonOperator parse(String operator) {
        var ctx = ParseContext.of(new TestSpecs());
        var cur = Cursor.of(operator, ctx.identifierQuoting());
        return parser.parse(cur, ctx);
    }
}
