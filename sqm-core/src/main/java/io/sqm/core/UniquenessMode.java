package io.sqm.core;

/** Semantic uniqueness mode for a MODEL clause. */
public enum UniquenessMode {
    /** Require unique dimension keys. */
    DIMENSION,
    /** Check uniqueness of single-cell reads. */
    SINGLE_REFERENCE
}
