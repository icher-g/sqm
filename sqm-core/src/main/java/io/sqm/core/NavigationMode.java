package io.sqm.core;

/** Semantic navigation mode for a MODEL clause. */
public enum NavigationMode {
    /** Keep nulls for absent or null cells. */
    KEEP,
    /** Use type-specific defaults for absent or null cells. */
    IGNORE
}
