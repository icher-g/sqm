package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.List;
import java.util.Objects;

/**
 * An array constructor expression.
 * <p>
 * Represents array construction syntax typically rendered as {@code ARRAY[...]} in PostgreSQL.
 * The elements are modeled as expressions, allowing literals, columns, function calls, casts, and more.
 *
 * <p>Examples (PostgreSQL)</p>
 * <ul>
 *   <li>{@code ARRAY['a', 'b']}</li>
 *   <li>{@code ARRAY[1, 2, 3]}</li>
 *   <li>{@code ARRAY[CAST(x AS int), y]}</li>
 * </ul>
 */
public non-sealed interface ArrayExpr extends Expression {

    /**
     * Creates an array constructor expression.
     *
     * @param elements element expressions
     * @return array constructor expression
     */
    static ArrayExpr of(List<Expression> elements) {
        return new Impl(elements);
    }

    /**
     * Creates an array constructor expression.
     *
     * @param elements element expressions
     * @return array constructor expression
     */
    static ArrayExpr of(Expression... elements) {
        return new Impl(List.of(elements));
    }

    /**
     * Array elements.
     *
     * @return immutable list of element expressions
     */
    List<Expression> elements();

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
        return v.visitArrayExpr(this);
    }

    /**
     * Default implementation.
     * <p>
     * Nested to keep the model change self-contained. You may later move it to your standard
     * implementation package without changing the public API.
     *
     * @param elements a list of expressions.
     */
    record Impl(List<Expression> elements) implements ArrayExpr {

        public Impl {
            Objects.requireNonNull(elements, "elements");
            if (elements.isEmpty()) {
                throw new IllegalArgumentException("elements must not be empty");
            }
            for (var e : elements) {
                Objects.requireNonNull(e, "elements must not contain nulls");
            }
            elements = List.copyOf(elements);
        }
    }
}
