package io.sqm.core;

/** Semantic rule order for a MODEL clause. */
public enum RuleOrder {
    /** Evaluate rules in declaration order. */
    SEQUENTIAL,
    /** Evaluate rules in dependency order. */
    AUTOMATIC
}
