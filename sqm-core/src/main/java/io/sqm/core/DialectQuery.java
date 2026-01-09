package io.sqm.core;

/**
 * Marker interface for dialect-specific {@link Query} nodes.
 * <p>
 * Use for query constructs that are not part of the ANSI core model
 * or for dialect-specific query forms/clauses that cannot be expressed
 * using existing ANSI nodes.
 * </p>
 */
public non-sealed interface DialectQuery extends Query, DialectNode {
}

