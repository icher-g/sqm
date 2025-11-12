package io.sqm.core.match;

import io.sqm.core.BoundSpec;
import io.sqm.core.Expression;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BoundSpecMatchTest {

    @Test
    void preceding() {
        var spec = BoundSpec.preceding(Expression.literal(1));
        String out = Match
            .<String>boundSpec(spec)
            .following(f -> "F")
            .preceding(p -> "P")
            .unboundedFollowing(f -> "UF")
            .unboundedPreceding(p -> "UP")
            .currentRow(r -> "R")
            .otherwise(s -> "ELSE");

        assertEquals("P", out);
    }

    @Test
    void following() {
        var spec = BoundSpec.following(Expression.literal(1));
        String out = Match
            .<String>boundSpec(spec)
            .currentRow(r -> "R")
            .preceding(p -> "P")
            .unboundedFollowing(f -> "UF")
            .unboundedPreceding(p -> "UP")
            .following(f -> "F")
            .otherwise(s -> "ELSE");

        assertEquals("F", out);
    }

    @Test
    void currentRow() {
        var spec = BoundSpec.currentRow();
        String out = Match
            .<String>boundSpec(spec)
            .currentRow(r -> "R")
            .following(f -> "F")
            .preceding(p -> "P")
            .unboundedFollowing(f -> "UF")
            .unboundedPreceding(p -> "UP")
            .otherwise(s -> "ELSE");

        assertEquals("R", out);
    }

    @Test
    void unboundedPreceding() {
        var spec = BoundSpec.unboundedPreceding();
        String out = Match
            .<String>boundSpec(spec)
            .currentRow(r -> "R")
            .following(f -> "F")
            .preceding(p -> "P")
            .unboundedFollowing(f -> "UF")
            .unboundedPreceding(p -> "UP")
            .otherwise(s -> "ELSE");

        assertEquals("UP", out);
    }

    @Test
    void unboundedFollowing() {
        var spec = BoundSpec.unboundedFollowing();
        String out = Match
            .<String>boundSpec(spec)
            .currentRow(r -> "R")
            .following(f -> "F")
            .preceding(p -> "P")
            .unboundedFollowing(f -> "UF")
            .unboundedPreceding(p -> "UP")
            .otherwise(s -> "ELSE");

        assertEquals("UF", out);
    }

    @Test
    void otherwise() {
        var spec = BoundSpec.unboundedFollowing();
        String out = Match
            .<String>boundSpec(spec)
            .currentRow(r -> "R")
            .following(f -> "F")
            .preceding(p -> "P")
            .unboundedPreceding(p -> "UP")
            .otherwise(s -> "ELSE");

        assertEquals("ELSE", out);
    }
}