package io.sqm.core;

/** Semantic return rows for a MODEL clause. */
public enum ReturnRows {
    /** Return all model rows. */
    ALL,
    /** Return rows changed by model rules. */
    UPDATED
}
