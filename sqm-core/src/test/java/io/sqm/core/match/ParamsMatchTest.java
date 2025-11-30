package io.sqm.core.match;

import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.param;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ParamsMatchTest {

    @Test
    void match_else() {
        var expr = param("n");
        String out = Match
            .<String>param(expr)
            .anonymous(p -> "A")
            .ordinal(p -> "O")
            .orElse("ELSE");

        assertEquals("ELSE", out);
    }

    @Test
    void match_anonymous() {
        var expr = param();
        String out = Match
            .<String>param(expr)
            .anonymous(p -> "A")
            .named(p -> "N")
            .ordinal(p -> "O")
            .orElse("ELSE");

        assertEquals("A", out);
    }

    @Test
    void match_ordinal() {
        var expr = param(1);
        String out = Match
            .<String>param(expr)
            .named(p -> "N")
            .ordinal(p -> "O")
            .anonymous(p -> "A")
            .orElse("ELSE");

        assertEquals("O", out);
    }

    @Test
    void match_named() {
        var expr = param("n");
        String out = Match
            .<String>param(expr)
            .anonymous(p -> "A")
            .ordinal(p -> "O")
            .named(p -> "N")
            .orElse("ELSE");

        assertEquals("N", out);
    }
}