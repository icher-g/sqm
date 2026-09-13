package io.sqm.core.match;

import io.sqm.core.CellAddress;
import io.sqm.core.CellFor;
import io.sqm.core.CellSelector;

import java.util.Objects;
import java.util.function.Function;

/**
 * Matches CellAddress variants in registration order.
 *
 * @param <R> result type
 */
public interface CellAddressMatch<R> extends Match<CellAddress, R> {
    /**
     * Creates a matcher.
     *
     * @param node subject
     * @param <R>  result type
     * @return matcher
     */
    static <R> CellAddressMatch<R> match(CellAddress node) {
        return new Impl<>(node);
    }

    /**
     * Registers a selector handler.
     *
     * @param handler variant handler
     * @return this matcher
     */
    CellAddressMatch<R> selector(Function<CellSelector, R> handler);

    /**
     * Registers a generator handler.
     *
     * @param handler variant handler
     * @return this matcher
     */
    CellAddressMatch<R> generator(Function<CellFor, R> handler);

    /**
     * Internal matcher state.
     *
     * @param <R> result type
     */
    final class Impl<R> implements CellAddressMatch<R> {
        private final CellAddress node;
        private boolean matched;
        private R result;

        private Impl(CellAddress node) {
            this.node = Objects.requireNonNull(node, "node");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public CellAddressMatch<R> selector(Function<CellSelector, R> handler) {
            if (!matched && node instanceof CellSelector value) {
                result = handler.apply(value);
                matched = true;
            }
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public CellAddressMatch<R> generator(Function<CellFor, R> handler) {
            if (!matched && node instanceof CellFor value) {
                result = handler.apply(value);
                matched = true;
            }
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public R otherwise(Function<CellAddress, R> handler) {
            return matched ? result : handler.apply(node);
        }
    }
}