package io.sqm.core.match;

import io.sqm.core.*;
import java.util.Objects;
import java.util.function.Function;

/**
 * Matches CellSelector variants in registration order.
 * @param <R> result type
 */
public interface CellSelectorMatch<R> extends Match<CellSelector, R> {
    /**
     * Creates a matcher.
     * @param node subject
     * @param <R> result type
     * @return matcher
     */
    static <R> CellSelectorMatch<R> match(CellSelector node) {
        return new Impl<>(node);
    }
    /**
     * Registers a value handler.
     * @param handler variant handler
     * @return this matcher
     */
    CellSelectorMatch<R> value(Function<CellSelector.Value, R> handler);
    /**
     * Registers a condition handler.
     * @param handler variant handler
     * @return this matcher
     */
    CellSelectorMatch<R> condition(Function<CellSelector.Condition, R> handler);
    /**
     * Internal matcher state.
     * @param <R> result type
     */
    final class Impl<R> implements CellSelectorMatch<R> {
        private final CellSelector node;
        private boolean matched;
        private R result;
        private Impl(CellSelector node) {
            this.node = Objects.requireNonNull(node, "node");
        }
        /** {@inheritDoc} */
        @Override
        public CellSelectorMatch<R> value(Function<CellSelector.Value, R> handler) {
            if (!matched && node instanceof CellSelector.Value value) {
                result = handler.apply(value);
                matched = true;
            }
            return this;
        }
        /** {@inheritDoc} */
        @Override
        public CellSelectorMatch<R> condition(Function<CellSelector.Condition, R> handler) {
            if (!matched && node instanceof CellSelector.Condition value) {
                result = handler.apply(value);
                matched = true;
            }
            return this;
        }
        /** {@inheritDoc} */
        @Override
        public R otherwise(Function<CellSelector, R> handler) {
            return matched ? result : handler.apply(node);
        }
    }
}
