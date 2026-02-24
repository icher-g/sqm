package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * Represents an expression-level {@code COLLATE} operation.
 * <p>
 * Example: {@code name COLLATE "de-CH"}
 */
public non-sealed interface CollateExpr extends Expression {

    /**
     * Creates a {@link CollateExpr} wrapping the provided expression and collation qualified name.
     *
     * @param expr      expression to apply collation to
     * @param collation qualified collation identifier
     * @return a new {@link CollateExpr}
     */
    static CollateExpr of(Expression expr, QualifiedName collation) {
        return new Impl(expr, collation);
    }

    /**
     * Expression being collated.
     *
     * @return expression
     */
    Expression expr();

    /**
     * Collation name.
     *
     * @return collation name
     */
    QualifiedName collation();

    /**
     * Accepts a {@link NodeVisitor} and dispatches control to the
     * visitor method corresponding to the concrete subtype.
     *
     * @param v   the visitor instance to accept (must not be {@code null})
     * @param <R> the result type returned by the visitor
     * @return the result produced by the visitor
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitCollateExpr(this);
    }

    /**
     * Default implementation.
     *
     * @param expr          expression to collate
     * @param collation collation name
     */
    record Impl(Expression expr, QualifiedName collation) implements CollateExpr {

        /**
         * Creates a new {@link CollateExpr} implementation.
         *
         * @param expr          expression to collate
         * @param collation collation name
         */
        public Impl {
            Objects.requireNonNull(expr, "expr");
            Objects.requireNonNull(collation, "collation");
            if (collation.parts().isEmpty()) {
                throw new IllegalArgumentException("collation must not be empty");
            }
        }
    }
}
