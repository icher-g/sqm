package io.sqm.core.match;

import io.sqm.core.ColumnExpr;
import io.sqm.core.LiteralExpr;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;


public class ExpressionMatchTest {

    @Test
    void kase_forSpecificSubtype_isChosen() {
        var col = ColumnExpr.of("c");
        var out = Match
                      .<String>expressions(col)
                      .func(f -> "FUNC")
                      .column(c -> "COL")
                      .otherwise(e -> "OTHER");
        assertEquals("COL", out);
    }

    @Test
    void multiple_kases_firstMatchWins() {
        var kase = kase(when(col("u", "name").gt(10)).then(col("o", "name")));
        String out = ExpressionMatch
                         .<String>match(kase)
                         .func(f -> "FUNC")
                         .kase(c -> "CASE")
                         .otherwise(e -> "OTHER");
        assertEquals("CASE", out);
    }

    @Test
    void otherwise_variants_work() {
        LiteralExpr lit = lit(10);

        String v1 = ExpressionMatch
                        .<String>match(lit)
                        .kase(c -> "NOPE")
                        .orElse("DEF");
        assertEquals("DEF", v1);

        String v2 = ExpressionMatch
                        .<String>match(lit)
                        .kase(c -> "NOPE")
                        .orElseGet(() -> "SUP");
        assertEquals("SUP", v2);

        assertThrows(IllegalStateException.class, () ->
                                                      ExpressionMatch
                                                          .<String>match(lit)
                                                          .kase(c -> "NOPE")
                                                          .orElseThrow(IllegalStateException::new)
        );

        assertTrue(ExpressionMatch
                       .<String>match(lit)
                       .kase(c -> "NOPE")
                       .otherwiseEmpty()
                       .isEmpty());
    }
}
