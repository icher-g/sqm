package io.cherlabs.sqlmodel.render.spi;

public enum PlaceholderPreference {
    AUTO,       // pick the “best” supported by the dialect
    POSITIONAL, // "?"
    ORDINAL,    // "$1", "$2"...
    NAMED       // ":p1", "@p1", etc.
}
