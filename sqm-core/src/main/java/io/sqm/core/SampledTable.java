package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

/**
 * Table wrapper that applies sampling to its source relation.
 */
public non-sealed interface SampledTable extends TableRef {
    /**
     * Creates a sampled table.
     *
     * @param source source table reference
     * @param sampleSpec sampling specification
     * @return sampled table
     */
    static SampledTable of(TableRef source, TableSampleSpec sampleSpec) {
        return of(source, sampleSpec, null);
    }

    /**
     * Creates a sampled table.
     *
     * @param source source table reference
     * @param sampleSpec sampling specification
     * @param alias optional alias
     * @return sampled table
     */
    static SampledTable of(TableRef source, TableSampleSpec sampleSpec, Identifier alias) {
        return new Impl(source, sampleSpec, alias);
    }

    /**
     * Returns the sampled source relation.
     *
     * @return source relation
     */
    TableRef source();

    /**
     * Returns the sampling specification.
     *
     * @return table sample specification
     */
    TableSampleSpec sampleSpec();

    /**
     * Returns the optional sampled-table alias.
     *
     * @return alias or {@code null}
     */
    Identifier alias();

    /**
     * Creates a copy with an alias.
     *
     * @param alias table alias
     * @return sampled table with alias
     */
    default SampledTable as(Identifier alias) {
        return of(source(), sampleSpec(), alias);
    }

    /**
     * Accepts a node visitor.
     *
     * @param v visitor
     * @param <R> visitor result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitSampledTable(this);
    }

    /**
     * Immutable sampled table implementation.
     *
     * @param source source relation
     * @param sampleSpec sampling specification
     * @param alias optional alias
     */
    record Impl(TableRef source, TableSampleSpec sampleSpec, Identifier alias) implements SampledTable {
        /**
         * Creates an immutable sampled table.
         */
        public Impl {
            java.util.Objects.requireNonNull(source, "source");
            java.util.Objects.requireNonNull(sampleSpec, "sampleSpec");
        }
    }
}
