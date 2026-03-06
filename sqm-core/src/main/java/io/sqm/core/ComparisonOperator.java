package io.sqm.core;

import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;

import java.util.Objects;

/**
 * A set of operators used in {@link ComparisonPredicate}.
 */
public enum ComparisonOperator {
    /**
     * Equal
     */
    EQ(null),
    /**
     * Null-safe equal (for example MySQL {@code <=>})
     */
    NULL_SAFE_EQ(SqlFeature.NULL_SAFE_EQUALITY_PREDICATE),
    /**
     * Not equal
     */
    NE(null),
    /**
     * Less than
     */
    LT(null),
    /**
     * Less than equal
     */
    LTE(null),
    /**
     * Greater than
     */
    GT(null),
    /**
     * Greater than equal
     */
    GTE(null);

    private final SqlFeature requiredFeature;

    ComparisonOperator(SqlFeature requiredFeature) {
        this.requiredFeature = requiredFeature;
    }

    /**
     * Returns the dialect capability required by this operator, if any.
     *
     * @return required feature or {@code null} when universally supported.
     */
    public SqlFeature requiredFeature() {
        return requiredFeature;
    }

    /**
     * Checks whether this operator is supported by the provided capabilities.
     *
     * @param capabilities dialect capabilities.
     * @return {@code true} if supported.
     */
    public boolean isSupported(DialectCapabilities capabilities) {
        Objects.requireNonNull(capabilities, "capabilities");
        return requiredFeature == null || capabilities.supports(requiredFeature);
    }

    /**
     * Asserts this operator is supported for parsing.
     *
     * @param capabilities dialect capabilities.
     * @throws UnsupportedOperationException if operator is unsupported.
     */
    public void assertSupported(DialectCapabilities capabilities) {
        if (!isSupported(capabilities)) {
            throw new UnsupportedOperationException(requiredFeature.description() + " is not supported by this dialect");
        }
    }

    /**
     * Asserts this operator is supported for rendering.
     *
     * @param capabilities dialect capabilities.
     * @param dialectName  dialect name for error reporting.
     * @throws UnsupportedDialectFeatureException if operator is unsupported.
     */
    public void assertSupported(DialectCapabilities capabilities, String dialectName) {
        if (!isSupported(capabilities)) {
            throw new UnsupportedDialectFeatureException(requiredFeature.description(), dialectName);
        }
    }
}
