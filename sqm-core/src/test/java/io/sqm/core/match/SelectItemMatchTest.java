package io.sqm.core.match;

import io.sqm.core.ExprSelectItem;
import io.sqm.core.QualifiedStarSelectItem;
import io.sqm.core.StarSelectItem;
import io.sqm.dsl.Dsl;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SelectItemMatchTest {

    @Test
    void select_item_cases() {
        ExprSelectItem exprItem = sel(lit("n"));
        String v1 = SelectItemMatch
                        .<String>match(exprItem)
                        .star(s -> "STAR")
                        .qualifiedStar(s -> "QSTAR")
                        .expr(e -> "EXPR")
                        .otherwise(s -> "OTHER");
        assertEquals("EXPR", v1);

        StarSelectItem star = Dsl.star();
        String v2 = SelectItemMatch
                        .<String>match(star)
                        .expr(e -> "EXPR")
                        .qualifiedStar(s -> "QSTAR")
                        .star(s -> "STAR")
                        .otherwise(s -> "OTHER");
        assertEquals("STAR", v2);
    }

    @Test
    void otherwise_helpers() {
        QualifiedStarSelectItem qstar = star("a");
        String v = SelectItemMatch
                       .<String>match(qstar)
                       .expr(e -> "NOPE")
                       .orElse("DEF");
        assertEquals("DEF", v);
    }
}
