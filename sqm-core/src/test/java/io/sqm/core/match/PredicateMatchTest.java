package io.sqm.core.match;

import io.sqm.core.AndPredicate;
import io.sqm.core.BetweenPredicate;
import io.sqm.core.ExistsPredicate;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class PredicateMatchTest {

    @Test
    void matches_specificPredicateSubtype() {
        BetweenPredicate between = col("a").between(1, 2);

        String out = PredicateMatch
            .<String>match(between)
            .comparison(p -> "CMP")
            .between(p -> "BET")
            .otherwise(p -> "OTHER");

        assertEquals("BET", out);
    }

    @Test
    void composite_predicates_example() {
        AndPredicate andP = col("a")
            .in(1, 2)
            .and(col("b").eq(1));

        String out = PredicateMatch
            .<String>match(andP)
            .or(p -> "OR")
            .and(p -> "AND")
            .otherwise(p -> "OTHER");

        assertEquals("AND", out);
    }

    @Test
    void fallback_helpers_behave() {
        ExistsPredicate exists = exists(select(lit(1)));

        String v = PredicateMatch
            .<String>match(exists)
            .in(p -> "NOPE")
            .orElse("D");
        assertEquals("D", v);
    }
}
