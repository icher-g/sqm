package io.sqm.core.match;

import io.sqm.core.Identifier;
import io.sqm.core.OverSpec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static io.sqm.dsl.Dsl.over;

class OverSpecMatchTest {

    @Test
    void ref() {
        var ref = over("w");
        String out = Match
            .<String>overSpec(ref)
            .def(d -> "D")
            .ref(r -> "R")
            .orElse("ELSE");

        assertEquals("R", out);
    }

    @Test
    void def() {
        var def = over("w", null, null, null);
        String out = Match
            .<String>overSpec(def)
            .ref(r -> "R")
            .def(d -> "D")
            .ref(r -> "R")
            .orElse("ELSE");

        assertEquals("D", out);
    }

    @Test
    void otherwise() {
        var def = over("w", null, null, null);
        String out = Match
            .<String>overSpec(def)
            .ref(r -> "R")
            .otherwise(s ->"ELSE");

        assertEquals("ELSE", out);
    }

    @Test
    void otherwiseEmpty() {
        var def = OverSpec.def(Identifier.of("w"), null, null, null);
        var out = Match
            .<String>overSpec(def)
            .ref(r -> "R")
            .otherwiseEmpty();

        assertTrue(out.isEmpty());
    }
}
