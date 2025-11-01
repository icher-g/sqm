package io.sqm.core.walk;

import io.sqm.core.CteDef;
import io.sqm.core.WhenThen;

/**
 * A generic visitor for traversing and processing statement-level components
 * of the SQM model such as {@link WhenThen} clauses and common table expression
 * (CTE) definitions.
 * <p>
 * This interface follows the standard visitor pattern used throughout the SQM
 * model. Implementations provide behavior for specific statement node types by
 * overriding the corresponding {@code visit*} methods.
 * </p>
 *
 * <p>
 * Typical usages include:
 * </p>
 * <ul>
 *   <li>Rendering statement components to SQL strings.</li>
 *   <li>Analyzing or validating parts of a query definition.</li>
 *   <li>Transforming or rewriting {@link WhenThen} clauses or {@link CteDef}
 *       nodes as part of query optimization or normalization.</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * public final class CollectCteNames implements StatementVisitor<Void> {
 *
 *     private final List<String> names = new ArrayList<>();
 *
 *     @Override
 *     public Void visitWhenThen(WhenThen w) {
 *         // No-op for CASE expressions
 *         return null;
 *     }
 *
 *     @Override
 *     public Void visitCte(CteDef c) {
 *         names.add(c.name());
 *         return null;
 *     }
 *
 *     public List<String> getNames() {
 *         return names;
 *     }
 * }
 * }</pre>
 *
 * @param <R> the return type produced by this visitor, which allows both
 *            data-producing visitors (e.g., {@code String}, {@code Boolean})
 *            and side-effect visitors (using {@code Void})
 * @see WhenThen
 * @see CteDef
 */
public interface StatementVisitor<R> {

    /**
     * Visits a single {@link WhenThen} clause within a {@link io.sqm.core.Expression}
     * such as a {@code CASE WHEN ... THEN ...} construct.
     *
     * @param w the {@link WhenThen} clause being visited
     * @return a result specific to the visitor implementation
     */
    R visitWhenThen(WhenThen w);

    /**
     * Visits a {@link CteDef} (Common Table Expression definition) node used in
     * {@code WITH} queries to define reusable subqueries.
     *
     * @param c the CTE definition being visited
     * @return a result specific to the visitor implementation
     */
    R visitCte(CteDef c);
}

