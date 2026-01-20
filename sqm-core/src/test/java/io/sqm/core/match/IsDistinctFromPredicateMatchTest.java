package io.sqm.core.match;

import io.sqm.core.IsDistinctFromPredicate;
import io.sqm.core.Predicate;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link IsDistinctFromPredicate} match API integration.
 */
class IsDistinctFromPredicateMatchTest {

    @Test
    void testMatchIsDistinctFrom() {
        Predicate pred = IsDistinctFromPredicate.of(col("a"), lit(10), false);
        
        String result = Match.<String>predicate(pred)
            .isDistinctFrom(p -> "IS DISTINCT FROM matched")
            .orElse("not matched");
        
        assertEquals("IS DISTINCT FROM matched", result);
    }

    @Test
    void testMatchIsNotDistinctFrom() {
        Predicate pred = IsDistinctFromPredicate.of(col("x"), col("y"), true);
        
        String result = Match.<String>predicate(pred)
            .isDistinctFrom(p -> {
                assertTrue(p.negated());
                return "IS NOT DISTINCT FROM matched";
            })
            .orElse("not matched");
        
        assertEquals("IS NOT DISTINCT FROM matched", result);
    }

    @Test
    void testMatchWithOtherPredicateTypes() {
        Predicate isDistinct = IsDistinctFromPredicate.of(col("a"), lit(1), false);
        Predicate comparison = col("b").eq(lit(2));
        Predicate isNull = col("c").isNull();
        
        assertEquals("distinct", Match.<String>predicate(isDistinct)
            .isDistinctFrom(p -> "distinct")
            .comparison(p -> "comparison")
            .isNull(p -> "null")
            .orElse("other"));
        
        assertEquals("comparison", Match.<String>predicate(comparison)
            .isDistinctFrom(p -> "distinct")
            .comparison(p -> "comparison")
            .isNull(p -> "null")
            .orElse("other"));
        
        assertEquals("null", Match.<String>predicate(isNull)
            .isDistinctFrom(p -> "distinct")
            .comparison(p -> "comparison")
            .isNull(p -> "null")
            .orElse("other"));
    }

    @Test
    void testMatchExtractsOperands() {
        var lhs = col("left");
        var rhs = lit("right");
        Predicate pred = IsDistinctFromPredicate.of(lhs, rhs, false);
        
        String result = Match.<String>predicate(pred)
            .isDistinctFrom(p -> {
                assertSame(lhs, p.lhs());
                assertSame(rhs, p.rhs());
                assertFalse(p.negated());
                return "validated";
            })
            .orElse("failed");
        
        assertEquals("validated", result);
    }

    @Test
    void testMatchOrElse() {
        Predicate pred = col("a").eq(lit(1));
        
        String result = Match.<String>predicate(pred)
            .isDistinctFrom(p -> "distinct")
            .orElse("other");
        
        assertEquals("other", result);
    }

    @Test
    void testMatchOrElseGet() {
        Predicate pred = IsDistinctFromPredicate.of(col("x"), lit(5), false);
        
        String result = Match.<String>predicate(pred)
            .comparison(p -> "comparison")
            .orElseGet(() -> "default");
        
        assertEquals("default", result);
    }

    @Test
    void testMatchOrElseThrow() {
        Predicate pred = IsDistinctFromPredicate.of(col("a"), lit(1), false);
        
        assertDoesNotThrow(() -> {
            Match.<String>predicate(pred)
                .isDistinctFrom(p -> "found")
                .orElseThrow(() -> new RuntimeException("Not found"));
        });
    }

    @Test
    void testMatchOrElseThrowWhenNotMatched() {
        Predicate pred = col("a").eq(lit(1));
        
        assertThrows(RuntimeException.class, () -> Match.<String>predicate(pred)
            .isDistinctFrom(p -> "found")
            .orElseThrow(() -> new RuntimeException("Not found")));
    }

    @Test
    void testMatchOtherwiseEmpty() {
        Predicate pred = IsDistinctFromPredicate.of(col("a"), lit(1), false);
        
        var result = Match.<String>predicate(pred)
            .isDistinctFrom(p -> "matched")
            .otherwiseEmpty();
        
        assertTrue(result.isPresent());
        assertEquals("matched", result.get());
    }

    @Test
    void testMatchOtherwiseEmptyWhenNotMatched() {
        Predicate pred = col("a").eq(lit(1));
        
        var result = Match.<String>predicate(pred)
            .isDistinctFrom(p -> "matched")
            .otherwiseEmpty();
        
        assertTrue(result.isEmpty());
    }

    @Test
    void testMatchChaining() {
        Predicate pred = IsDistinctFromPredicate.of(col("a"), lit(1), false);
        
        String result = Match.<String>predicate(pred)
            .comparison(p -> "comparison")
            .in(p -> "in")
            .isDistinctFrom(p -> "distinct")
            .like(p -> "like")
            .orElse("none");
        
        assertEquals("distinct", result);
    }

    @Test
    void testMatchFirstWins() {
        Predicate pred = IsDistinctFromPredicate.of(col("a"), lit(1), false);
        
        String result = Match.<String>predicate(pred)
            .isDistinctFrom(p -> "first")
            .isDistinctFrom(p -> "second")
            .orElse("none");
        
        assertEquals("first", result, "First matching handler should win");
    }

    @Test
    void testMatchReturnsNull() {
        Predicate pred = IsDistinctFromPredicate.of(col("a"), lit(1), false);
        
        String result = Match.<String>predicate(pred)
            .isDistinctFrom(p -> null)
            .orElse("default");
        
        assertNull(result, "Handler can return null");
    }

    @Test
    void testMatchComplexPredicate() {
        var pred1 = IsDistinctFromPredicate.of(col("a"), lit(1), false);
        var pred2 = col("b").eq(lit(2));
        var composite = pred1.and(pred2);
        
        String result = Match.<String>predicate(composite)
            .and(p -> "and: " + p.lhs() + ", " + p.rhs())
            .orElse("not and");
        
        assertTrue(result.startsWith("and:"));
    }

    @Test
    void testMatchNegatedPredicate() {
        var pred = IsDistinctFromPredicate.of(col("x"), lit(5), false);
        var notPred = pred.not();
        
        String result = Match.<String>predicate(notPred)
            .not(p -> "negated")
            .orElse("other");
        
        assertEquals("negated", result);
    }

    @Test
    void testMatchWithDifferentReturnTypes() {
        Predicate pred = IsDistinctFromPredicate.of(col("a"), lit(10), false);
        
        Integer intResult = Match.<Integer>predicate(pred)
            .isDistinctFrom(p -> 42)
            .orElse(0);
        
        assertEquals(42, intResult);
        
        Boolean boolResult = Match.<Boolean>predicate(pred)
            .isDistinctFrom(p -> true)
            .orElse(false);
        
        assertTrue(boolResult);
    }

    @Test
    void testMatchStaticFactory() {
        var pred = IsDistinctFromPredicate.of(col("a"), lit(1), false);
        
        String result = Match.<String>predicate(pred)
            .isDistinctFrom(p -> "static match")
            .orElse("failed");
        
        assertEquals("static match", result);
    }
}
