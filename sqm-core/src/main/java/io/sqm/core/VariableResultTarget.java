package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.List;
import java.util.Objects;

/**
 * Represents a DML result target made of host or procedural variables.
 * <p>
 * This is used by dialects such as Oracle where {@code RETURNING ... INTO}
 * assigns result expressions into bind variables instead of returning rows to
 * the caller as a direct result set.
 */
public non-sealed interface VariableResultTarget extends ResultTarget {

    /**
     * Creates a variable result target.
     *
     * @param variables target bind variables
     * @return variable result target
     */
    static VariableResultTarget of(List<? extends ParamExpr> variables) {
        return new Impl(variables.stream().map(ParamExpr.class::cast).toList());
    }

    /**
     * Returns the target variables.
     *
     * @return immutable target variable list
     */
    List<ParamExpr> variables();

    /**
     * Accepts a visitor.
     *
     * @param v visitor instance
     * @param <R> result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitVariableResultTarget(this);
    }

    /**
     * Default immutable implementation.
     *
     * @param variables target bind variables
     */
    record Impl(List<ParamExpr> variables) implements VariableResultTarget {

        /**
         * Creates a variable result target implementation.
         *
         * @param variables target bind variables
         */
        public Impl {
            Objects.requireNonNull(variables, "variables");
            variables = List.copyOf(variables);
            if (variables.isEmpty()) {
                throw new IllegalArgumentException("variables must not be empty");
            }
        }
    }
}
