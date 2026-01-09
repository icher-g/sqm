package io.sqm.core;

/**
 * Marker interface for dialect-specific {@link Predicate} nodes.
 * <p>
 * Implementations represent boolean conditions that are not defined
 * by ANSI SQL and are only available in specific SQL dialects.
 * </p>
 *
 * <p>
 * Dialect predicates allow the SQM core model to remain strictly
 * ANSI-compliant while still enabling full fidelity modeling of
 * vendor-specific SQL features.
 * </p>
 *
 * <p>
 * Examples of dialect predicates may include:
 * </p>
 * <ul>
 *   <li>PostgreSQL <code>IS DISTINCT FROM</code></li>
 *   <li>PostgreSQL <code>ILIKE</code> predicate</li>
 *   <li>Vendor-specific comparison or matching operators</li>
 * </ul>
 *
 * <p>
 * Dialect-aware visitors, transformers, and renderers must explicitly
 * handle these predicates. Generic ANSI components may reject them
 * during validation or rendering.
 * </p>
 *
 * <p>
 * This interface is {@code non-sealed} to enable unrestricted extension
 * by dialect-specific modules.
 * </p>
 */
public non-sealed interface DialectPredicate extends Predicate, DialectNode {
}
