package io.sqm.core;

/**
 * Marker interface for dialect-specific {@link Join} nodes.
 * <p>
 * Use for join forms, join modifiers, or join conditions that are
 * not covered by the ANSI join types in the core model.
 * </p>
 */
public non-sealed interface DialectJoin extends Join, DialectNode {
}

