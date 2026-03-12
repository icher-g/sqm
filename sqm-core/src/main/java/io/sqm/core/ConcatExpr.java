package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.List;
import java.util.Objects;

/**
 * A string concatenation expression.
 * <p>
 * Represents dialect-neutral concatenation of two or more expressions.
 * Dialects may render this semantic node using infix operators such as
 * {@code ||} or function syntax such as {@code CONCAT(...)}.
 *
 * <p>Examples</p>
 * <ul>
 *   <li>{@code first_name || ' ' || last_name}</li>
 *   <li>{@code CONCAT(first_name, ' ', last_name)}</li>
 * </ul>
 */
public non-sealed interface ConcatExpr extends Expression {

    /**
     * Creates a concatenation expression from a list of arguments.
     *
     * @param args concatenated expressions
     * @return concatenation expression
     */
    static ConcatExpr of(List<Expression> args) {
        return new Impl(args);
    }

    /**
     * Creates a concatenation expression from an array of arguments.
     *
     * @param args concatenated expressions
     * @return concatenation expression
     */
    static ConcatExpr of(Expression... args) {
        Objects.requireNonNull(args, "args");
        return of(List.of(args));
    }

    /**
     * Concatenated expressions in evaluation order.
     *
     * @return immutable argument list
     */
    List<Expression> args();

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
        return v.visitConcatExpr(this);
    }

    /**
     * Default implementation.
     *
     * @param args concatenated expressions
     */
    record Impl(List<Expression> args) implements ConcatExpr {

        /**
         * Creates a concatenation expression implementation.
         *
         * @param args concatenated expressions
         */
        public Impl {
            Objects.requireNonNull(args, "args");
            if (args.isEmpty()) {
                throw new IllegalArgumentException("args must not be empty");
            }
            for (Expression arg : args) {
                Objects.requireNonNull(arg, "args must not contain nulls");
            }
            args = List.copyOf(args);
        }
    }
}
