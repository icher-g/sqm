package io.sqm.core;

/** Semantic rule mode for a MODEL clause. */
public enum RuleMode {
    /** Update existing cells. */
    UPDATE,
    /** Update cells and insert eligible missing cells. */
    UPSERT,
    /** Apply upserts to mixed positional and symbolic targets. */
    UPSERT_ALL
}
