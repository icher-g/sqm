package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * Running or final evaluation mode applied to a row-pattern expression.
 */
public non-sealed interface PatternEvaluationExpr extends Expression {
    /**
     * Pattern evaluation mode.
     */
    enum Mode {
        /** Evaluates using the rows currently accumulated in the match. */
        RUNNING,
        /** Evaluates using the completed match. */
        FINAL
    }

    /**
     * Creates a pattern evaluation expression.
     *
     * @param mode evaluation mode
     * @param expression expression evaluated in that mode
     * @return immutable pattern evaluation expression
     */
    static PatternEvaluationExpr of(Mode mode, Expression expression) {
        return new Impl(mode, expression);
    }

    /**
     * Returns the evaluation mode.
     *
     * @return evaluation mode
     */
    Mode mode();

    /**
     * Returns the wrapped expression.
     *
     * @return wrapped expression
     */
    Expression expression();

    /**
     * Accepts a node visitor.
     *
     * @param visitor visitor to accept
     * @param <R> visitor result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitPatternEvaluationExpr(this);
    }

    /**
     * Immutable pattern-evaluation implementation.
     *
     * @param mode evaluation mode
     * @param expression expression evaluated in that mode
     */
    record Impl(Mode mode, Expression expression) implements PatternEvaluationExpr {
        /**
         * Validates the pattern evaluation expression.
         *
         * @param mode evaluation mode
         * @param expression expression evaluated in that mode
         */
        public Impl {
            Objects.requireNonNull(mode, "mode");
            Objects.requireNonNull(expression, "expression");
        }
    }
}
