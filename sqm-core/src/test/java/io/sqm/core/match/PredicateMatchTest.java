package io.sqm.core.match;

import io.sqm.core.BetweenPredicate;
import io.sqm.core.ComparisonOperator;
import io.sqm.core.ExistsPredicate;
import io.sqm.core.Predicate;
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
    void matches_andPredicate() {
        Predicate predicate = col("a")
            .in(1, 2)
            .and(col("b").eq(1));

        String out = PredicateMatch
            .<String>match(predicate)
            .comparison(p -> "CMP")
            .between(p -> "BET")
            .anyAll(p -> "AnyAll")
            .exists(p -> "EXISTS")
            .isNull(p -> "IsNull")
            .like(p -> "LIKE")
            .not(p -> "NOT")
            .unary(p -> "UNARY")
            .in(p -> "IN")
            .or(p -> "OR")
            .and(p -> "AND")
            .otherwise(p -> "OTHER");

        assertEquals("AND", out);
    }

    @Test
    void matches_orPredicate() {
        Predicate predicate = col("a")
            .in(1, 2)
            .or(col("b").eq(1));

        String out = Match
            .<String>predicate(predicate)
            .comparison(p -> "CMP")
            .between(p -> "BET")
            .anyAll(p -> "AnyAll")
            .exists(p -> "EXISTS")
            .isNull(p -> "IsNull")
            .like(p -> "LIKE")
            .not(p -> "NOT")
            .unary(p -> "UNARY")
            .in(p -> "IN")
            .and(p -> "AND")
            .or(p -> "OR")
            .orElseThrow(IllegalArgumentException::new);

        assertEquals("OR", out);
    }

    @Test
    void fallback_helpers_behave() {
        ExistsPredicate exists = exists(select(lit(1)).build());

        String v = PredicateMatch
            .<String>match(exists)
            .or(p -> "OR")
            .in(p -> "NOPE")
            .orElse("D");
        assertEquals("D", v);
    }

    @Test
    void matches_anyAllPredicate() {
        var predicate = col("c").any(ComparisonOperator.EQ, select(lit(1)).build());
        String out = PredicateMatch
            .<String>match(predicate)
            .anyAll(p -> "AnyAll")
            .otherwise(p -> "OTHER");
        assertEquals("AnyAll", out);
    }

    @Test
    void matches_existsPredicate() {
        var predicate = exists(select(lit(1)).build());
        String out = PredicateMatch
            .<String>match(predicate)
            .exists(p -> "EXISTS")
            .otherwise(p -> "OTHER");
        assertEquals("EXISTS", out);
    }

    @Test
    void matches_isNullPredicate() {
        var predicate = col("c").isNull();
        String out = PredicateMatch
            .<String>match(predicate)
            .isNull(p -> "IsNull")
            .otherwise(p -> "OTHER");
        assertEquals("IsNull", out);
    }

    @Test
    void matches_likePredicate() {
        var predicate = col("c").like("%c%");
        String out = PredicateMatch
            .<String>match(predicate)
            .like(p -> "LIKE")
            .otherwise(p -> "OTHER");
        assertEquals("LIKE", out);
    }

    @Test
    void matches_notPredicate() {
        var predicate = col("c").like("%c%").not();
        String out = PredicateMatch
            .<String>match(predicate)
            .not(p -> "NOT")
            .otherwise(p -> "OTHER");
        assertEquals("NOT", out);
    }

    @Test
    void matches_unaryPredicate() {
        var predicate = unary(lit(true));
        String out = PredicateMatch
            .<String>match(predicate)
            .unary(p -> "UNARY")
            .otherwise(p -> "OTHER");
        assertEquals("UNARY", out);
    }

    @Test
    void matches_comparisonPredicate() {
        var predicate = col("c").eq(1);
        String out = PredicateMatch
            .<String>match(predicate)
            .comparison(p -> "COMPARISON")
            .otherwise(p -> "OTHER");
        assertEquals("COMPARISON", out);
    }

    @Test
    void matches_inPredicate() {
        var predicate = col("c").in(1, 2, 3);
        String out = PredicateMatch
            .<String>match(predicate)
            .in(p -> "IN")
            .otherwise(p -> "OTHER");
        assertEquals("IN", out);
    }
}
