package io.sqm.core.transform;

import io.sqm.core.Predicate;
import io.sqm.core.UnaryPredicate;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link BooleanPredicateSimplifier}.
 *
 * <p>Tests use DSL helpers so examples match intended developer usage.</p>
 */
class BooleanPredicateSimplifierTest {

    private static Predicate simplify(Predicate predicate) {
        return (Predicate) predicate.accept(new BooleanPredicateSimplifier());
    }

    @Test
    void eliminates_double_not() {
        var original = not(col("active").eq(true).not());

        var simplified = simplify(original);

        assertEquals(col("active").eq(true), simplified);
    }

    @Test
    void simplifies_and_or_with_boolean_literals() {
        var p = col("active").eq(true);
        var original = unary(lit(true)).and(p).or(unary(lit(false)));

        var simplified = simplify(original);

        assertEquals(p, simplified);
    }

    @Test
    void simplifies_not_true_and_not_false() {
        var notTrue = simplify(not(unary(lit(true))));
        var notFalse = simplify(not(unary(lit(false))));

        assertInstanceOf(UnaryPredicate.class, notTrue);
        assertInstanceOf(UnaryPredicate.class, notFalse);
        assertEquals(Boolean.FALSE, ((io.sqm.core.LiteralExpr) ((UnaryPredicate) notTrue).expr()).value());
        assertEquals(Boolean.TRUE, ((io.sqm.core.LiteralExpr) ((UnaryPredicate) notFalse).expr()).value());
    }

    @Test
    void preserves_null_boolean_logic_by_not_folding_null_literals() {
        var p = unary(lit(null)).and(col("active").eq(true));

        var simplified = simplify(p);

        assertEquals(p, simplified);
    }

    @Test
    void leaves_non_boolean_predicates_unchanged() {
        var predicate = col("a").eq(1);

        var simplified = simplify(predicate);

        assertSame(predicate, simplified);
    }

    @Test
    void folds_and_or_when_both_sides_are_boolean_literals() {
        var andPred = unary(lit(true)).and(unary(lit(false)));
        var orPred = unary(lit(false)).or(unary(lit(true)));

        var andOut = simplify(andPred);
        var orOut = simplify(orPred);

        assertEquals(Boolean.FALSE, ((io.sqm.core.LiteralExpr) ((UnaryPredicate) andOut).expr()).value());
        assertEquals(Boolean.TRUE, ((io.sqm.core.LiteralExpr) ((UnaryPredicate) orOut).expr()).value());
    }

    @Test
    void simplifies_unary_predicate_when_inner_predicate_expression_changes() {
        var unaryPredicate = unary(not(not(col("flag").eq(true))));

        var simplified = simplify(unaryPredicate);

        assertInstanceOf(UnaryPredicate.class, simplified);
        assertEquals(col("flag").eq(true), ((UnaryPredicate) simplified).expr());
    }
}
