package io.sqm.core;

/**
 * Quantifier used by {@code ANY}/{@code ALL} predicates.
 */
public enum Quantifier {
    /** Quantifier meaning all values must satisfy the condition. */
    ALL,
    /** Quantifier meaning any value may satisfy the condition. */
    ANY
}
