package io.sqm.core.match;

import io.sqm.core.Join;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.inner;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JoinMatchTest {

    @Test
    void on() {
        var join = inner(tbl("t"));
        String out = Match.
            <String>join(join)
            .using(u -> "U")
            .natural(n -> "N")
            .cross(c -> "C")
            .on(o -> "O")
            .otherwise(s -> "ELSE");

        assertEquals("O", out);
    }

    @Test
    void using() {
        var join = Join.inner(tbl("t")).using("a");
        String out = Match.
            <String>join(join)
            .using(u -> "U")
            .natural(n -> "N")
            .cross(c -> "C")
            .on(o -> "O")
            .otherwise(s -> "ELSE");

        assertEquals("U", out);
    }

    @Test
    void natural() {
        var join = Join.natural(tbl("t"));
        String out = Match.
            <String>join(join)
            .natural(n -> "N")
            .cross(c -> "C")
            .on(o -> "O")
            .using(u -> "U")
            .otherwise(s -> "ELSE");

        assertEquals("N", out);
    }

    @Test
    void cross() {
        var join = Join.cross(tbl("t"));
        String out = Match.
            <String>join(join)
            .on(o -> "O")
            .using(u -> "U")
            .natural(n -> "N")
            .cross(c -> "C")
            .otherwise(s -> "ELSE");

        assertEquals("C", out);
    }

    @Test
    void orElseThrow() {
        var join = Join.cross(tbl("t"));
        assertThrows(IllegalArgumentException.class,
            () -> Match.
                <String>join(join)
                .natural(n -> "N")
                .orElseThrow(IllegalArgumentException::new));
    }

    @Test
    void otherwise() {
        var join = Join.cross(tbl("t"));
        String out = Match.
            <String>join(join)
            .on(o -> "O")
            .using(u -> "U")
            .natural(n -> "N")
            .otherwise(s -> "ELSE");

        assertEquals("ELSE", out);
    }
}