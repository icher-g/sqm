package io.sqm.core.match;

import io.sqm.core.*;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AtTimeZoneExpr} match support in {@link ExpressionMatch}.
 * <p>
 * These tests verify that:
 * <ul>
 *     <li>The {@code atTimeZone()} method correctly matches AT TIME ZONE expressions</li>
 *     <li>Non-matching expressions fall through to alternative handlers</li>
 *     <li>All match API variants work correctly (orElse, orElseGet, orElseThrow, etc.)</li>
 * </ul>
 */
class AtTimeZoneExprMatchTest {

    @Test
    void atTimeZone_matchesAtTimeZoneExpr() {
        Expression expr = AtTimeZoneExpr.of(col("ts"), lit("UTC"));

        String result = Match
            .<String>expression(expr)
            .column(c -> "COLUMN")
            .atTimeZone(a -> "AT_TIME_ZONE")
            .otherwise(e -> "OTHER");

        assertEquals("AT_TIME_ZONE", result);
    }

    @Test
    void atTimeZone_withComplexTimestampExpr() {
        Expression expr = AtTimeZoneExpr.of(
            func("now", starArg()),
            lit("America/New_York")
        );

        String result = Match
            .<String>expression(expr)
            .func(f -> "FUNC")
            .atTimeZone(a -> {
                assertInstanceOf(FunctionExpr.class, a.timestamp());
                return "AT_TIME_ZONE";
            })
            .otherwise(e -> "OTHER");

        assertEquals("AT_TIME_ZONE", result);
    }

    @Test
    void atTimeZone_nonMatching_usesOrElse() {
        Expression expr = col("name");

        String result = Match
            .<String>expression(expr)
            .atTimeZone(a -> "AT_TIME_ZONE")
            .orElse("DEFAULT");

        assertEquals("DEFAULT", result);
    }

    @Test
    void atTimeZone_nonMatching_usesOrElseGet() {
        Expression expr = lit(42);

        String result = Match
            .<String>expression(expr)
            .atTimeZone(a -> "AT_TIME_ZONE")
            .orElseGet(() -> "LAZY_DEFAULT");

        assertEquals("LAZY_DEFAULT", result);
    }

    @Test
    void atTimeZone_nonMatching_usesOtherwiseEmpty() {
        Expression expr = col("status");

        var result = Match
            .<String>expression(expr)
            .atTimeZone(a -> "AT_TIME_ZONE")
            .otherwiseEmpty();

        assertTrue(result.isEmpty());
    }

    @Test
    void atTimeZone_orElseThrow() {
        Expression expr = col("value");

        assertThrows(IllegalStateException.class, () ->
            Match
                .<String>expression(expr)
                .atTimeZone(a -> "AT_TIME_ZONE")
                .orElseThrow(IllegalStateException::new)
        );
    }

    @Test
    void atTimeZone_firstMatchWins() {
        Expression expr = AtTimeZoneExpr.of(col("ts"), lit("UTC"));

        String result = Match
            .<String>expression(expr)
            .column(c -> "COLUMN")
            .atTimeZone(a -> "AT_TIME_ZONE")
            .literal(l -> "LITERAL")
            .otherwise(e -> "OTHER");

        // atTimeZone should match before falling through to other handlers
        assertEquals("AT_TIME_ZONE", result);
    }

    @Test
    void atTimeZone_extractsTimestampAndTimezone() {
        AtTimeZoneExpr expr = AtTimeZoneExpr.of(col("created_at"), lit("UTC"));

        String result = Match
            .<String>expression(expr)
            .atTimeZone(atz -> {
                String ts = Match.<String>expression(atz.timestamp())
                    .column(c -> c.name())
                    .orElse("unknown");
                String tz = Match.<String>expression(atz.timezone())
                    .literal(l -> String.valueOf(l.value()))
                    .orElse("unknown");
                return ts + " -> " + tz;
            })
            .otherwise(e -> "NOT_AT_TIME_ZONE");

        assertEquals("created_at -> UTC", result);
    }

    @Test
    void atTimeZone_withNestedAtTimeZone() {
        // Inner: ts AT TIME ZONE 'UTC'
        AtTimeZoneExpr inner = AtTimeZoneExpr.of(col("ts"), lit("UTC"));
        // Outer: (ts AT TIME ZONE 'UTC') AT TIME ZONE user_tz
        Expression expr = AtTimeZoneExpr.of(inner, col("user_tz"));

        String result = Match
            .<String>expression(expr)
            .atTimeZone(outer -> {
                // The timestamp is itself an AT TIME ZONE expression
                return Match.<String>expression(outer.timestamp())
                    .atTimeZone(inner2 -> "NESTED")
                    .orElse("NOT_NESTED");
            })
            .otherwise(e -> "OTHER");

        assertEquals("NESTED", result);
    }

    @Test
    void atTimeZone_multipleAlternatives() {
        Expression expr = col("name");

        String result = Match
            .<String>expression(expr)
            .kase(k -> "CASE")
            .cast(c -> "CAST")
            .atTimeZone(a -> "AT_TIME_ZONE")
            .column(c -> "COLUMN")
            .otherwise(e -> "OTHER");

        assertEquals("COLUMN", result);
    }

    @Test
    void atTimeZone_withOrElseThrow() {
        Expression expr = col("name");

        assertThrows(IllegalArgumentException.class, () -> Match
            .<String>expression(expr)
            .atTimeZone(a -> "AT_TIME_ZONE")
            .orElseThrow(() -> new IllegalArgumentException("Expected AT TIME ZONE")));
    }

    @Test
    void atTimeZone_withOrElseGetSupplier() {
        Expression expr = col("name");

        String result = Match
            .<String>expression(expr)
            .atTimeZone(a -> "MATCHED")
            .orElseGet(() -> "SUPPLIED_DEFAULT");

        assertEquals("SUPPLIED_DEFAULT", result);
    }

    @Test
    void atTimeZone_doesNotMatchOtherExpressions() {
        Expression expr = func("now", starArg());

        String result = Match
            .<String>expression(expr)
            .atTimeZone(a -> "AT_TIME_ZONE")
            .func(f -> "FUNCTION")
            .orElse("OTHER");

        assertEquals("FUNCTION", result);
    }

    @Test
    void atTimeZone_matchedFlagPreventsDouble() {
        AtTimeZoneExpr expr = AtTimeZoneExpr.of(col("ts"), lit("UTC"));

        String result = Match
            .<String>expression(expr)
            .atTimeZone(a -> "FIRST")
            .atTimeZone(a -> "SECOND")  // Should not match again
            .otherwise(e -> "OTHER");

        assertEquals("FIRST", result);
    }
}
