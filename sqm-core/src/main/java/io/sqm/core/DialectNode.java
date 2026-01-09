package io.sqm.core;

/**
 * Marker interface for any dialect-specific SQM node.
 * <p>
 * Useful for validation, renderer gating (ANSI renderer can reject),
 * and visitors that want a single instanceof check.
 * </p>
 */
public non-sealed interface DialectNode extends Node {
}

