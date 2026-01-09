package io.sqm.core;

/**
 * Marker interface for dialect-specific {@link Expression} nodes.
 * <p>
 * Implementations of this interface represent SQL expressions that are
 * not part of the ANSI SQL standard and are only supported by specific
 * SQL dialects (for example PostgreSQL, MySQL, SQL Server).
 * </p>
 *
 * <p>
 * Dialect expressions are intentionally excluded from the core SQM
 * model to keep it ANSI-compliant and portable. Dialect-aware parsers,
 * transformers, validators, and renderers are expected to recognize
 * and handle these nodes explicitly.
 * </p>
 *
 * <p>
 * Typical examples include:
 * </p>
 * <ul>
 *   <li>PostgreSQL-specific expressions (e.g. <code>ILIKE</code>,
 *       <code>jsonb</code> operators, array access)</li>
 *   <li>Vendor-specific functions or operators</li>
 *   <li>Non-standard type casts or syntactic constructs</li>
 * </ul>
 *
 * <p>
 * This interface is declared {@code non-sealed} to allow dialect modules
 * to freely define their own expression hierarchies without modifying
 * the core SQM module.
 * </p>
 */
public non-sealed interface DialectExpression extends Expression, DialectNode {
}

