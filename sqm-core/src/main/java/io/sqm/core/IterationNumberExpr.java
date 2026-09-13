package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;
import java.util.Objects;

/**
 * The zero-based iteration number of a model calculation.
 *
 * <p>This node represents {@code ITERATION_NUMBER}, without function-call parentheses.</p>
 * <pre>{@code
 * SELECT step, pass_number
 * FROM DUAL
 * MODEL
 *   DIMENSION BY (1 AS step)
 *   MEASURES (0 AS pass_number)
 *   RULES ITERATE (3) (
 *     pass_number[1] = ITERATION_NUMBER
 *   )
 * }</pre>
 */
public non-sealed interface IterationNumberExpr extends Expression {
    /**
     * Creates an immutable IterationNumberExpr with its complete state.
     *
     * @return immutable node
     */
    static IterationNumberExpr of() {
        return new Impl();
    }

    /** {@inheritDoc} */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitIterationNumberExpr(this);
    }

    /**
     * Immutable implementation.
     *
     */
    record Impl() implements IterationNumberExpr {
        /**
         * Validates and copies the supplied state.
         *
         */
        public Impl {
        }
    }
}
