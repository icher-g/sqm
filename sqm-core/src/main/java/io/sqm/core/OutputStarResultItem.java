package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * SQL Server {@code OUTPUT} star projection using a pseudo-row source.
 * <p>
 * Examples:
 * <ul>
 *     <li>{@code OUTPUT inserted.*}</li>
 *     <li>{@code OUTPUT deleted.*}</li>
 * </ul>
 */
public non-sealed interface OutputStarResultItem extends ResultItem {

    /**
     * Creates an {@code inserted.*} result item.
     *
     * @return inserted star result item
     */
    static OutputStarResultItem inserted() {
        return of(OutputRowSource.INSERTED);
    }

    /**
     * Creates a {@code deleted.*} result item.
     *
     * @return deleted star result item
     */
    static OutputStarResultItem deleted() {
        return of(OutputRowSource.DELETED);
    }

    /**
     * Creates an output-star result item.
     *
     * @param source SQL Server pseudo-row source
     * @return output-star result item
     */
    static OutputStarResultItem of(OutputRowSource source) {
        return new Impl(source);
    }

    /**
     * Returns the SQL Server pseudo-row source.
     *
     * @return output row source
     */
    OutputRowSource source();

    /**
     * Accepts a visitor.
     *
     * @param v visitor instance
     * @param <R> result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitOutputStarResultItem(this);
    }

    /**
     * Default immutable implementation.
     *
     * @param source SQL Server pseudo-row source
     */
    record Impl(OutputRowSource source) implements OutputStarResultItem {

        /**
         * Creates an output-star result item implementation.
         *
         * @param source SQL Server pseudo-row source
         */
        public Impl {
            Objects.requireNonNull(source, "source");
        }
    }
}
