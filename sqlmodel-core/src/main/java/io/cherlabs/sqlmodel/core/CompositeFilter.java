package io.cherlabs.sqlmodel.core;

import io.cherlabs.sqlmodel.core.traits.HasCompositeOperator;
import io.cherlabs.sqlmodel.core.traits.HasFilters;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a composite on. A list of filters joined by one of the operators (OR, AND).
 *
 * @param operator An op to be used between the filters.
 * @param filters  A list of filters.
 */
public record CompositeFilter(Operator operator, List<Filter> filters) implements Filter, HasCompositeOperator, HasFilters {
    public enum Operator {
        And,
        Or,
        Not
    }

    public CompositeFilter where(Filter filter) {
        if (operator == Operator.Not) {
            return new CompositeFilter(operator, List.of(filter));
        }

        var list = new ArrayList<>(filters);
        list.add(filter);
        return new CompositeFilter(operator, list);
    }
}
