package io.sqm.core;

/**
 * Marker interface for dialect-specific {@link TableRef} nodes.
 * <p>
 * Implementations represent table references that are not part of the
 * ANSI SQL standard and are only supported by specific SQL dialects.
 * </p>
 *
 * <p>
 * Typical use cases include:
 * </p>
 * <ul>
 *   <li>Dialect-specific table reference syntax</li>
 *   <li>Special table functions or pseudo-tables</li>
 *   <li>Non-standard modifiers applied to table references</li>
 * </ul>
 *
 * <p>
 * Dialect-aware renderers, validators, and visitors are expected to
 * explicitly handle these nodes. ANSI-only components may reject them
 * or report unsupported features.
 * </p>
 *
 * <p>
 * This interface is declared {@code non-sealed} to allow dialect modules
 * to define their own table reference hierarchies without modifying the
 * SQM core model.
 * </p>
 */
public non-sealed interface DialectTableRef extends TableRef, DialectNode {
}

