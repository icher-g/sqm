package io.sqm.core.match;

import io.sqm.core.*;
import java.util.Objects;
import java.util.function.Function;

/**
 * Matches CellFor variants in registration order.
 * @param <R> result type
 */
public interface CellForMatch<R> extends Match<CellFor, R> {
    /**
     * Creates a matcher.
     * @param node subject
     * @param <R> result type
     * @return matcher
     */
    static <R> CellForMatch<R> match(CellFor node) {
        return new Impl<>(node);
    }
    /**
     * Registers a values handler.
     * @param handler variant handler
     * @return this matcher
     */
    CellForMatch<R> values(Function<CellFor.Values, R> handler);
    /**
     * Registers a range handler.
     * @param handler variant handler
     * @return this matcher
     */
    CellForMatch<R> range(Function<CellFor.Range, R> handler);
    /**
     * Internal matcher state.
     * @param <R> result type
     */
    final class Impl<R> implements CellForMatch<R> {
        private final CellFor node;
        private boolean matched;
        private R result;
        private Impl(CellFor node) {
            this.node = Objects.requireNonNull(node, "node");
        }
        /** {@inheritDoc} */
        @Override
        public CellForMatch<R> values(Function<CellFor.Values, R> handler) {
            if (!matched && node instanceof CellFor.Values value) {
                result = handler.apply(value);
                matched = true;
            }
            return this;
        }
        /** {@inheritDoc} */
        @Override
        public CellForMatch<R> range(Function<CellFor.Range, R> handler) {
            if (!matched && node instanceof CellFor.Range value) {
                result = handler.apply(value);
                matched = true;
            }
            return this;
        }
        /** {@inheritDoc} */
        @Override
        public R otherwise(Function<CellFor, R> handler) {
            return matched ? result : handler.apply(node);
        }
    }
}
