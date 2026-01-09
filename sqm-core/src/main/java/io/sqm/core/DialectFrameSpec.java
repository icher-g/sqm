package io.sqm.core;

/**
 * Marker interface for dialect-specific {@link FrameSpec} nodes.
 * <p>
 * Implementations represent window frame specifications that are not
 * defined by the ANSI SQL standard or that introduce dialect-specific
 * behavior.
 * </p>
 *
 * <p>
 * Examples may include:
 * </p>
 * <ul>
 *   <li>Non-standard frame modes or keywords</li>
 *   <li>Dialect-specific frame defaults</li>
 *   <li>Extensions to {@code ROWS}, {@code RANGE}, or {@code GROUPS}
 *       semantics</li>
 * </ul>
 *
 * <p>
 * Dialect-aware renderers and validators are responsible for ensuring
 * correct interpretation and SQL generation for these nodes.
 * </p>
 *
 * <p>
 * This interface is {@code non-sealed} to enable dialect modules to
 * introduce new frame specification variants without altering the
 * core model.
 * </p>
 */
public non-sealed interface DialectFrameSpec extends FrameSpec, DialectNode {
}

