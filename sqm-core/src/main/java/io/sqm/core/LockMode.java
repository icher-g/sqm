package io.sqm.core;

/**
 * Row-level lock modes for SELECT locking clauses.
 *
 * <p>Not all SQL dialects support all lock modes. Dialect-specific parsers,
 * renderers, and validators decide which modes are allowed.</p>
 */
public enum LockMode {
    /**
     * Corresponds to a mode commonly spelled as {@code FOR UPDATE}.
     */
    UPDATE,

    /**
     * Corresponds to a mode commonly spelled as {@code FOR NO KEY UPDATE}.
     */
    NO_KEY_UPDATE,

    /**
     * Corresponds to a mode commonly spelled as {@code FOR SHARE}.
     */
    SHARE,

    /**
     * Corresponds to a mode commonly spelled as {@code FOR KEY SHARE}.
     */
    KEY_SHARE
}


