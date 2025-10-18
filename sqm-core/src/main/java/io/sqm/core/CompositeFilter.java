package io.sqm.core;

import io.sqm.core.traits.HasCompositeOperator;
import io.sqm.core.traits.HasFilters;

import java.util.List;

/**
 * Represents a composite on. A list of filters joined by one of the operators (OR, AND).
 *
 * @param op An op to be used between the filters.
 * @param filters  A list of filters.
 */
public record CompositeFilter(Operator op, List<Filter> filters) implements Filter, HasCompositeOperator, HasFilters {
    public enum Operator {
        And,
        Or,
        Not
    }
}
