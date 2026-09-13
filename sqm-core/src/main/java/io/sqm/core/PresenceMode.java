package io.sqm.core;

/** Semantic presence mode for a MODEL clause. */
public enum PresenceMode {
    /** Test whether the input cell exists. */
    CELL,
    /** Test existence and a non-null value. */
    NON_NULL_VALUE
}
