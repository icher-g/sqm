package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;
import java.util.Objects;

/**
 * The iteration limit and optional termination condition for model rules.
 *
 * <p>This node represents {@code ITERATE (10) UNTIL (PREVIOUS(balance[1]) = balance[1])}; the following
 * assignments belong to {@link ModelRules}.</p>
 * <pre>{@code
 * SELECT step, balance
 * FROM DUAL
 * MODEL
 *   DIMENSION BY (1 AS step)
 *   MEASURES (100 AS balance)
 *   RULES ITERATE (10) UNTIL (PREVIOUS(balance[1]) = balance[1]) (
 *     balance[1] = ROUND(balance[1] * 0.9)
 *   )
 * }</pre>
 */
public non-sealed interface IterationSpec extends Node {
    /**
     * Creates an immutable IterationSpec with its complete state.
     *
     * @param limit iteration limit
     * @param until optional termination condition; {@code null} when absent
     * @return immutable node
     */
    static IterationSpec of(Expression limit, Predicate until) {
        return new Impl(limit, until);
    }

    /**
     * Returns the iteration limit.
     *
     * @return iteration limit
     */
    Expression limit();

    /**
     * Returns the optional termination condition.
     *
     * @return optional termination condition; {@code null} when absent
     */
    Predicate until();

    /** {@inheritDoc} */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitIterationSpec(this);
    }

    /**
     * Immutable implementation.
     *
     * @param limit iteration limit
     * @param until optional termination condition; {@code null} when absent
     */
    record Impl(Expression limit, Predicate until) implements IterationSpec {
        /**
         * Validates and copies the supplied state.
         *
         * @param limit iteration limit
         * @param until optional termination condition; {@code null} when absent
         */
        public Impl {
            Objects.requireNonNull(limit, "limit");
            io.sqm.core.internal.ModelChecks.positiveLiteral(limit, true, "iteration limit");
        }
    }
}
