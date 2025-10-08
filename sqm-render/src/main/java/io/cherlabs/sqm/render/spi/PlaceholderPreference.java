package io.cherlabs.sqm.render.spi;

/**
 * A list of supported types of placeholders.
 */
public enum PlaceholderPreference {
    Auto,       // pick the “best” supported by the dialect
    Positional, // "?"
    Ordinal,    // "$1", "$2"...
    Named       // ":p1", "@p1", etc.
}
