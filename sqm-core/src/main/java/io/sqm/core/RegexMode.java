package io.sqm.core;

/**
 * Specifies the regular expression matching mode used by
 * {@link RegexPredicate}.
 *
 * <p>This enum models operator-level regular expression matching behavior,
 * such as case sensitivity. The regular expression pattern itself is not
 * modified by SQM.</p>
 *
 * @see RegexPredicate
 */
public enum RegexMode {

    /**
     * Case-sensitive regular expression match.
     */
    MATCH,

    /**
     * Case-insensitive regular expression match.
     */
    MATCH_INSENSITIVE
}



