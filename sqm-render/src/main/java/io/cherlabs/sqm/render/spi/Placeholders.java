package io.cherlabs.sqm.render.spi;

/**
 * How parameter placeholders are written in SQL for a given dialect.
 */
public interface Placeholders {
    /**
     * Plain positional marker (e.g., "?" or "$1" if ordinal is used).
     */
    String marker();

    /**
     * Does the dialect require/want ordinal markers like $1, $2 ... ?
     */
    default boolean supportsOrdinal() {
        return false;
    }

    /**
     * Ordinal placeholder for 1-based position (e.g., "$1").
     */
    default String ordinal(int position) {
        throw new UnsupportedOperationException("Ordinal placeholders not supported by this dialect");
    }

    /**
     * Does the dialect support named placeholders like ":name" or "@p1"?
     */
    default boolean supportsNamed() {
        return false;
    }

    /**
     * Named placeholder token (e.g., ":name").
     */
    default String named(String name) {
        throw new UnsupportedOperationException("Named placeholders not supported by this dialect");
    }
}
