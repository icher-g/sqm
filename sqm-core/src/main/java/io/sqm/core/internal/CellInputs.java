package io.sqm.core.internal;

import io.sqm.core.*;

/** Shared conversions for the compact and fluent MODEL cell DSL. */
public final class CellInputs {
    private CellInputs() {
    }

    /**
     * Converts a coordinate into a selector, preserving predicates as conditions.
     * @param input selector, predicate, expression, or literal (including SQL NULL)
     * @return selector
     * @throws IllegalArgumentException if input is a non-expression SQL node
     */
    public static CellSelector selector(Object input) {
        if (input instanceof CellSelector selector) {
            return selector;
        }
        if (input instanceof Predicate predicate) {
            return CellSelector.Condition.of(predicate);
        }
        return value(input);
    }

    /**
     * Converts a coordinate into a writable address, allowing FOR generators.
     * @param input address, predicate, expression, or literal (including SQL NULL)
     * @return writable address
     * @throws IllegalArgumentException if input is an unsupported SQL node
     */
    public static CellAddress address(Object input) {
        return input instanceof CellAddress address ? address : selector(input);
    }

    /**
     * Explicitly wraps a positional value; predicates remain value expressions here.
     * @param input expression or literal; {@code null} represents SQL NULL
     * @return positional selector
     * @throws IllegalArgumentException if input is a non-expression SQL node
     */
    public static CellSelector.Value value(Object input) {
        if (input instanceof Expression expression) {
            return CellSelector.Value.of(expression);
        }
        if (input instanceof Node) {
            throw new IllegalArgumentException("Expected a cell expression or literal value");
        }
        return CellSelector.Value.of(Expression.literal(input));
    }
}
