package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * A predicate that wraps a boolean-valued {@link Expression}.
 * <p>
 * This predicate acts as a bridge between expression parsing and predicate grammar.
 * It allows expressions that evaluate to a boolean value to be used in predicate
 * contexts such as {@code WHERE}, {@code ON}, or {@code HAVING}.
 *
 * <p>
 * This is primarily intended for operator-heavy and dialect-specific constructs
 * that are naturally modeled as expressions but semantically behave as predicates.
 * Typical examples include PostgreSQL operators such as:
 *
 * <ul>
 *   <li>{@code data @> '{"a":1}'::jsonb}</li>
 *   <li>{@code data ? 'key'}</li>
 *   <li>{@code name ~* 'abc'}</li>
 *   <li>{@code tags && ARRAY['a','b']}</li>
 * </ul>
 *
 * <p>
 * {@code ExprPredicate} should be used as a fallback in predicate parsing,
 * after all structured predicate forms (comparison, {@code IN}, {@code BETWEEN},
 * {@code LIKE}, {@code IS NULL}, etc.) have been attempted.
 *
 * <p>
 * This node intentionally does not enforce that the wrapped expression is boolean.
 * Dialect-specific validation or type checking may be applied in later phases.
 */
public non-sealed interface ExprPredicate extends Predicate {

    /**
     * Creates a predicate that wraps the given expression.
     *
     * @param expr expression expected to evaluate to a boolean value
     * @return expression predicate
     */
    static ExprPredicate of(Expression expr) {
        return new Impl(expr);
    }

    /**
     * The wrapped expression that is expected to evaluate to a boolean value.
     *
     * @return boolean-valued expression
     */
    Expression expr();

    /**
     * Accepts a {@link NodeVisitor} and dispatches control to the
     * visitor method corresponding to the concrete subtype
     *
     * @param v   the visitor instance to accept (must not be {@code null})
     * @param <R> the result type returned by the visitor
     * @return the result produced by the visitor
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitExprPredicate(this);
    }

    /**
     * Default implementation.
     * <p>
     * Nested to keep the model change self-contained. You may later move it
     * to your standard implementation package without changing the public API.
     */
    record Impl(Expression expr) implements ExprPredicate {

        public Impl {
            Objects.requireNonNull(expr, "expr");
        }
    }
}
