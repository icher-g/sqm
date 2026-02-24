package io.sqm.core.transform;

/**
 * Case normalization mode for unquoted identifiers.
 *
 * <p>Quoted identifiers are preserved by identifier normalization and are not affected by this mode.</p>
 */
public enum IdentifierNormalizationCaseMode {
    /**
     * Lower-case unquoted identifiers.
     */
    LOWER,
    /**
     * Upper-case unquoted identifiers.
     */
    UPPER,
    /**
     * Keep unquoted identifiers unchanged.
     */
    UNCHANGED
}
