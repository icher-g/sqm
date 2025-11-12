package io.sqm.core.match;

import io.sqm.core.QualifiedStarSelectItem;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.star;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SelectItemMatchTest {

    @Test
    void match_expr() {
        var exprItem = lit("n").toSelectItem();
        String out = Match
            .<String>selectItem(exprItem)
            .expr(e -> "EXPR")
            .star(s -> "STAR")
            .qualifiedStar(s -> "QSTAR")
            .expr(e -> "EXPR")
            .otherwise(s -> "OTHER");
        assertEquals("EXPR", out);
    }

    @Test
    void match_star() {
        var star = star();
        String out = Match
            .<String>selectItem(star)
            .star(s -> "STAR")
            .expr(e -> "EXPR")
            .qualifiedStar(s -> "QSTAR")
            .star(s -> "STAR")
            .otherwise(s -> "OTHER");
        assertEquals("STAR", out);
    }

    @Test
    void match_qualifiedStar() {
        var star = star("a");
        String out = Match
            .<String>selectItem(star)
            .expr(e -> "EXPR")
            .star(s -> "STAR")
            .qualifiedStar(s -> "QSTAR")
            .otherwise(s -> "OTHER");
        assertEquals("QSTAR", out);
    }

    @Test
    void otherwise_helpers() {
        QualifiedStarSelectItem qstar = star("a");
        String v = Match
            .<String>selectItem(qstar)
            .expr(e -> "NOPE")
            .orElse("DEF");
        assertEquals("DEF", v);
    }
}
