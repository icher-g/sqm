package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

/**
 * Sampling specification applied to a table reference.
 */
public non-sealed interface TableSampleSpec extends Node {
    /**
     * Creates a table sampling specification.
     *
     * @param method sampling method
     * @param unit sample amount unit
     * @param amount sample amount expression
     * @param repeatableSeed optional repeatable seed expression
     * @return table sample specification
     */
    static TableSampleSpec of(SampleMethod method, SampleUnit unit, Expression amount, Expression repeatableSeed) {
        return new Impl(method, unit, amount, repeatableSeed);
    }

    /**
     * Returns the sampling method.
     *
     * @return sampling method
     */
    SampleMethod method();

    /**
     * Returns the sample amount unit.
     *
     * @return sample unit
     */
    SampleUnit unit();

    /**
     * Returns the sample amount expression.
     *
     * @return sample amount
     */
    Expression amount();

    /**
     * Returns the repeatable sampling seed.
     *
     * @return seed expression or {@code null}
     */
    Expression repeatableSeed();

    /**
     * Accepts a node visitor.
     *
     * @param v visitor
     * @param <R> visitor result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitTableSampleSpec(this);
    }

    /**
     * Table sampling method.
     */
    enum SampleMethod {
        /**
         * Dialect default sampling method.
         */
        DIALECT_DEFAULT,
        /**
         * Block/page-level sampling method.
         */
        BLOCK,
        /**
         * Bernoulli row-level sampling method.
         */
        BERNOULLI,
        /**
         * System-defined page/block sampling method.
         */
        SYSTEM
    }

    /**
     * Table sample amount unit.
     */
    enum SampleUnit {
        /**
         * Percentage of rows or blocks.
         */
        PERCENT,
        /**
         * Row-count sampling unit.
         */
        ROWS,
        /**
         * Unit is supplied by dialect syntax or left implicit.
         */
        UNSPECIFIED
    }

    /**
     * Immutable table sample specification implementation.
     *
     * @param method sampling method
     * @param unit sample amount unit
     * @param amount sample amount expression
     * @param repeatableSeed optional repeatable seed expression
     */
    record Impl(SampleMethod method, SampleUnit unit, Expression amount, Expression repeatableSeed) implements TableSampleSpec {
        /**
         * Creates an immutable table sample specification.
         */
        public Impl {
            method = method == null ? SampleMethod.DIALECT_DEFAULT : method;
            unit = unit == null ? SampleUnit.UNSPECIFIED : unit;
            java.util.Objects.requireNonNull(amount, "amount");
        }
    }
}
