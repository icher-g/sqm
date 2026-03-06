package io.sqm.core;

import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ComparisonOperator enum")
class ComparisonOperatorTest {

    @Test
    void includesNullSafeEqualityOperator() {
        assertEquals(ComparisonOperator.NULL_SAFE_EQ, ComparisonOperator.valueOf("NULL_SAFE_EQ"));
        assertNotNull(ComparisonOperator.NULL_SAFE_EQ);
    }

    @Test
    void allOperatorsAreDistinct() {
        ComparisonOperator[] operators = ComparisonOperator.values();
        assertEquals(7, operators.length);
        assertNotEquals(ComparisonOperator.EQ, ComparisonOperator.NE);
        assertNotEquals(ComparisonOperator.NULL_SAFE_EQ, ComparisonOperator.EQ);
        assertNotEquals(ComparisonOperator.NULL_SAFE_EQ, ComparisonOperator.NE);
    }

    @Test
    void operatorNameAndOrdinalAreStable() {
        assertEquals("EQ", ComparisonOperator.EQ.name());
        assertEquals("NULL_SAFE_EQ", ComparisonOperator.NULL_SAFE_EQ.name());
        assertEquals(0, ComparisonOperator.EQ.ordinal());
        assertEquals(1, ComparisonOperator.NULL_SAFE_EQ.ordinal());
    }

    @Test
    void valueOfRejectsInvalidOperatorName() {
        assertThrows(IllegalArgumentException.class, () -> ComparisonOperator.valueOf("INVALID"));
    }

    @Test
    void switchCoversAllOperators() {
        String description = switch (ComparisonOperator.NULL_SAFE_EQ) {
            case EQ -> "equal";
            case NULL_SAFE_EQ -> "null-safe equal";
            case NE -> "not equal";
            case LT -> "less than";
            case LTE -> "less than or equal";
            case GT -> "greater than";
            case GTE -> "greater than or equal";
        };

        assertEquals("null-safe equal", description);
    }

    @Test
    void valuesContainsAllOperators() {
        ComparisonOperator[] values = ComparisonOperator.values();
        assertTrue(contains(values, ComparisonOperator.EQ));
        assertTrue(contains(values, ComparisonOperator.NULL_SAFE_EQ));
        assertTrue(contains(values, ComparisonOperator.NE));
        assertTrue(contains(values, ComparisonOperator.LT));
        assertTrue(contains(values, ComparisonOperator.LTE));
        assertTrue(contains(values, ComparisonOperator.GT));
        assertTrue(contains(values, ComparisonOperator.GTE));
    }

    @Test
    void requiredFeatureIsNullForStandardOperators() {
        assertNull(ComparisonOperator.EQ.requiredFeature());
        assertNull(ComparisonOperator.NE.requiredFeature());
        assertNull(ComparisonOperator.LT.requiredFeature());
        assertNull(ComparisonOperator.LTE.requiredFeature());
        assertNull(ComparisonOperator.GT.requiredFeature());
        assertNull(ComparisonOperator.GTE.requiredFeature());
    }

    @Test
    void requiredFeatureIsSetForNullSafeEquality() {
        assertEquals(SqlFeature.NULL_SAFE_EQUALITY_PREDICATE, ComparisonOperator.NULL_SAFE_EQ.requiredFeature());
    }

    @Test
    void isSupportedDependsOnCapabilitiesForFeatureGatedOperator() {
        var supportsAll = (io.sqm.core.dialect.DialectCapabilities) feature -> true;
        var supportsNone = (io.sqm.core.dialect.DialectCapabilities) feature -> false;

        assertTrue(ComparisonOperator.NULL_SAFE_EQ.isSupported(supportsAll));
        assertFalse(ComparisonOperator.NULL_SAFE_EQ.isSupported(supportsNone));
        assertTrue(ComparisonOperator.EQ.isSupported(supportsNone));
    }

    @Test
    void assertSupportedRejectsNullCapabilities() {
        var error = assertThrows(NullPointerException.class, () -> ComparisonOperator.EQ.isSupported(null));
        assertEquals("capabilities", error.getMessage());
    }

    @Test
    void assertSupportedForParsingThrowsWhenFeatureIsUnsupported() {
        var supportsNone = (io.sqm.core.dialect.DialectCapabilities) feature -> false;

        var error = assertThrows(UnsupportedOperationException.class,
            () -> ComparisonOperator.NULL_SAFE_EQ.assertSupported(supportsNone));

        assertTrue(error.getMessage().contains("null-safe equality predicate"));
    }

    @Test
    void assertSupportedForRenderingThrowsDialectAwareExceptionWhenUnsupported() {
        var supportsNone = (io.sqm.core.dialect.DialectCapabilities) feature -> false;

        var error = assertThrows(UnsupportedDialectFeatureException.class,
            () -> ComparisonOperator.NULL_SAFE_EQ.assertSupported(supportsNone, "ANSI"));

        assertTrue(error.getMessage().contains("ANSI"));
    }

    private boolean contains(ComparisonOperator[] array, ComparisonOperator operator) {
        for (ComparisonOperator op : array) {
            if (op == operator) {
                return true;
            }
        }
        return false;
    }
}
