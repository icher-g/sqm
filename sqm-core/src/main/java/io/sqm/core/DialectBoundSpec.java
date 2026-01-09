package io.sqm.core;

/**
 * Marker interface for dialect-specific {@link BoundSpec} nodes.
 * <p>
 * Implementations represent window frame bounds that are not part of
 * the ANSI SQL specification or that alter standard bound semantics
 * in a dialect-specific manner.
 * </p>
 *
 * <p>
 * Typical examples include:
 * </p>
 * <ul>
 *   <li>Non-standard bound keywords</li>
 *   <li>Dialect-specific interpretations of preceding or following
 *       bounds</li>
 *   <li>Additional bound parameters or expressions</li>
 * </ul>
 *
 * <p>
 * Dialect-aware components must explicitly support these nodes.
 * ANSI-only components may reject them during validation or rendering.
 * </p>
 *
 * <p>
 * Declared {@code non-sealed} to allow dialect implementations to
 * freely define new bound variants.
 * </p>
 */
public non-sealed interface DialectBoundSpec extends BoundSpec, DialectNode {
}

