package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * A type cast expression.
 * <p>
 * Represents casting an expression to a SQL type, typically rendered as
 * {@code CAST(<expr> AS <type>)} or, in PostgreSQL, using shorthand syntax
 * such as {@code (<expr>)::type}.
 * <p>
 * The target type is stored as a raw string to keep the core model compact and dialect-agnostic.
 * Dialect modules may validate or normalize type names.
 *
 * <h3>Examples</h3>
 * <ul>
 *   <li>{@code CAST(amount AS bigint)}</li>
 *   <li>{@code '{"a":1}'::jsonb}</li>
 *   <li>{@code '{a,b,0}'::text[]}</li>
 * </ul>
 */
public non-sealed interface CastExpr extends Expression {

    /**
     * Creates a cast expression.
     *
     * @param expr expression to cast
     * @param type target type name (for example {@code "jsonb"} or {@code "text[]"})
     * @return cast expression
     */
    static CastExpr of(Expression expr, String type) {
        return new Impl(expr, type);
    }

    /**
     * Expression being cast.
     *
     * @return expression operand
     */
    Expression expr();

    /**
     * Target type name for the cast.
     * <p>
     * Stored as raw text (for example {@code "jsonb"}, {@code "text[]"}, {@code "bigint"}).
     *
     * @return target type name, not blank
     */
    String type();

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
        return v.visitCastExpr(this);
    }

    /**
     * Default implementation.
     * <p>
     * Nested to keep the model change self-contained. You may later move it to your standard
     * implementation package without changing the public API.
     */
    record Impl(Expression expr, String type) implements CastExpr {

        public Impl {
            Objects.requireNonNull(expr, "expr");
            Objects.requireNonNull(type, "type");
            if (type.isBlank()) {
                throw new IllegalArgumentException("type must not be blank");
            }
        }
    }
}
