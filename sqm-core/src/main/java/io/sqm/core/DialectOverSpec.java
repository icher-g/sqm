package io.sqm.core;

/**
 * Marker interface for dialect-specific {@link OverSpec} nodes.
 * <p>
 * Implementations represent window specification constructs that
 * extend or deviate from the ANSI SQL windowing model.
 * </p>
 *
 * <p>
 * Dialect extensions in this area are common and may include:
 * </p>
 * <ul>
 *   <li>Named window references or inline window definitions
 *       with dialect-specific options</li>
 *   <li>Additional clauses or modifiers within an {@code OVER (...)}
 *       specification</li>
 *   <li>Dialect-specific defaults or behaviors for window definitions</li>
 * </ul>
 *
 * <p>
 * Dialect-aware components must explicitly support these nodes.
 * Generic ANSI renderers may fail fast when encountering them.
 * </p>
 *
 * <p>
 * Declared {@code non-sealed} to allow unrestricted extension by
 * dialect-specific modules.
 * </p>
 */
public non-sealed interface DialectOverSpec extends OverSpec, DialectNode {
}

