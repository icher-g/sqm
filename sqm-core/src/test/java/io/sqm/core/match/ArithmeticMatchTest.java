package io.sqm.core.match;

import io.sqm.core.*;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static io.sqm.dsl.Dsl.lit;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ArithmeticMatch}.
 *
 * <p>These tests verify that:
 * <ul>
 *     <li>The correct branch is selected based on the concrete arithmetic
 *         expression type.</li>
 *     <li>{@code otherwise(...)} is used when no specific branch matches.</li>
 *     <li>Convenience methods like {@code otherwiseEmpty()}, {@code orElse(..)},
 *         and {@code orElseThrow(..)} behave as expected.</li>
 * </ul>
 */
class ArithmeticMatchTest {

    // -------------------------------------------------------------------------
    // Basic dispatch: successful matches
    // -------------------------------------------------------------------------

    @Test
    void add_branch_is_invoked_for_AddArithmeticExpr() {
        ArithmeticExpr expr = AddArithmeticExpr.of(lit(1), lit(2));

        String kind = ArithmeticMatch.<String>match(expr)
            .add(a -> "add")
            .sub(s -> "sub")
            .mul(m -> "mul")
            .div(d -> "div")
            .mod(mo -> "mod")
            .neg(n -> "neg")
            .otherwise(a -> "other");

        assertEquals("add", kind);
    }

    @Test
    void neg_branch_is_invoked_for_NegativeArithmeticExpr() {
        ArithmeticExpr expr = NegativeArithmeticExpr.of(lit(1));

        String kind = ArithmeticMatch.<String>match(expr)
            .add(a -> "add")
            .sub(s -> "sub")
            .mul(m -> "mul")
            .div(d -> "div")
            .mod(mo -> "mod")
            .neg(n -> "neg")
            .otherwise(a -> "other");

        assertEquals("neg", kind);
    }

    @Test
    void mod_branch_is_invoked_for_ModArithmeticExpr() {
        ArithmeticExpr expr = ModArithmeticExpr.of(lit(5), lit(2));

        String kind = ArithmeticMatch.<String>match(expr)
            .add(a -> "add")
            .sub(s -> "sub")
            .mul(m -> "mul")
            .div(d -> "div")
            .mod(mo -> "mod")
            .neg(n -> "neg")
            .otherwise(a -> "other");

        assertEquals("mod", kind);
    }

    @Test
    void pow_branch_is_invoked_for_PowerArithmeticExpr() {
        ArithmeticExpr expr = PowerArithmeticExpr.of(lit(2), lit(3));

        String kind = ArithmeticMatch.<String>match(expr)
            .add(a -> "add")
            .sub(s -> "sub")
            .mul(m -> "mul")
            .div(d -> "div")
            .mod(mo -> "mod")
            .neg(n -> "neg")
            .pow(p -> "pow")
            .otherwise(a -> "other");

        assertEquals("pow", kind);
    }

    @Test
    void pow_branch_is_skipped_when_already_matched() {
        ArithmeticExpr expr = AddArithmeticExpr.of(lit(1), lit(2));

        final int[] powCalls = {0};
        String kind = ArithmeticMatch.<String>match(expr)
            .add(a -> "add")
            .pow(p -> {
                powCalls[0]++;
                return "pow";
            })
            .otherwise(a -> "other");

        assertEquals("add", kind);
        assertEquals(0, powCalls[0]);
    }

    // -------------------------------------------------------------------------
    // Fallback: otherwise(...)
    // -------------------------------------------------------------------------

    @Test
    void otherwise_is_used_when_no_specific_branch_matches() {
        // Only 'sub' is registered, but we pass a MulArithmeticExpr.
        ArithmeticExpr expr = MulArithmeticExpr.of(lit(3), lit(4));

        String kind = ArithmeticMatch.<String>match(expr)
            .sub(s -> "sub")
            .otherwise(a -> "fallback");

        assertEquals("fallback", kind);
    }

    // -------------------------------------------------------------------------
    // otherwiseEmpty()
    // -------------------------------------------------------------------------

    @Test
    void otherwiseEmpty_returns_present_optional_when_branch_matches() {
        ArithmeticExpr expr = DivArithmeticExpr.of(lit(10), lit(2));

        Optional<String> result = ArithmeticMatch.<String>match(expr)
            .div(d -> "division")
            .otherwiseEmpty();

        assertTrue(result.isPresent());
        assertEquals("division", result.orElseThrow());
    }

    @Test
    void otherwiseEmpty_returns_empty_optional_when_no_branch_matches() {
        ArithmeticExpr expr = DivArithmeticExpr.of(lit(10), lit(2));

        Optional<String> result = ArithmeticMatch.<String>match(expr)
            .mod(m -> "mod")
            .otherwiseEmpty();

        assertTrue(result.isEmpty());
    }

    // -------------------------------------------------------------------------
    // orElse(...)
    // -------------------------------------------------------------------------

    @Test
    void orElse_uses_default_when_no_branch_matches() {
        ArithmeticExpr expr = SubArithmeticExpr.of(lit(10), lit(3));

        String value = ArithmeticMatch.<String>match(expr)
            .mod(m -> "mod")
            .orElse("default");

        assertEquals("default", value);
    }

    @Test
    void orElse_does_not_use_default_when_branch_matches() {
        ArithmeticExpr expr = SubArithmeticExpr.of(lit(10), lit(3));

        String value = ArithmeticMatch.<String>match(expr)
            .sub(s -> "subtraction")
            .orElse("default");

        assertEquals("subtraction", value);
    }

    // -------------------------------------------------------------------------
    // orElseGet(...)
    // -------------------------------------------------------------------------

    @Test
    void orElseGet_is_invoked_only_when_no_branch_matches() {
        ArithmeticExpr expr = SubArithmeticExpr.of(lit(10), lit(3));

        String value = ArithmeticMatch.<String>match(expr)
            .mul(m -> "mul")
            .orElseGet(() -> "computed-default");

        assertEquals("computed-default", value);
    }

    @Test
    void orElseGet_is_not_used_when_branch_matches() {
        ArithmeticExpr expr = SubArithmeticExpr.of(lit(10), lit(3));

        String value = ArithmeticMatch.<String>match(expr)
            .sub(s -> "subtraction")
            .orElseGet(() -> "computed-default");

        assertEquals("subtraction", value);
    }

    // -------------------------------------------------------------------------
    // orElseThrow(...)
    // -------------------------------------------------------------------------

    @Test
    void orElseThrow_throws_when_no_branch_matches() {
        ArithmeticExpr expr = SubArithmeticExpr.of(lit(10), lit(3));

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> ArithmeticMatch.<String>match(expr)
                .mul(m -> "mul")
                .orElseThrow(() -> new IllegalStateException("no match"))
        );

        assertEquals("no match", ex.getMessage());
    }

    @Test
    void orElseThrow_does_not_throw_when_branch_matches() {
        ArithmeticExpr expr = SubArithmeticExpr.of(lit(10), lit(3));

        String value = assertDoesNotThrow(
            () -> ArithmeticMatch.<String>match(expr)
                .sub(s -> "ok")
                .orElseThrow(() -> new IllegalStateException("no match"))
        );

        assertEquals("ok", value);
    }
}
