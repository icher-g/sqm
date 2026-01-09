package io.sqm.core;

/**
 * Marker interface for dialect-specific {@link FromItem} nodes.
 * <p>
 * Implementations represent non-ANSI elements that may appear in the
 * {@code FROM} clause and are supported only by specific SQL dialects.
 * </p>
 *
 * <p>
 * This enables modeling of vendor-specific {@code FROM} constructs
 * without introducing them into the ANSI core model.
 * </p>
 *
 * <p>
 * Typical examples include:
 * </p>
 * <ul>
 *   <li>PostgreSQL {@code LATERAL} joins</li>
 *   <li>Table-valued functions</li>
 *   <li>Dialect-specific sampling or partitioning clauses</li>
 * </ul>
 *
 * <p>
 * Dialect-aware renderers and validators are responsible for ensuring
 * correct handling and compatibility of these nodes.
 * </p>
 *
 * <p>
 * Declared {@code non-sealed} to allow dialect modules to define
 * arbitrarily complex {@code FROM} clause extensions.
 * </p>
 */
public non-sealed interface DialectFromItem extends FromItem, DialectNode {
}

