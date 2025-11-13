package io.sqm.core.internal;

import io.sqm.core.*;

import java.util.List;

/**
 * Represents a composite query.
 * <p>Example:</p>
 * <pre>
 *     {@code
 *     (SELECT * FROM TABLE1)
 *     UNION
 *     (SELECT * FROM TABLE2)
 *     INTERSECT
 *     (SELECT * FROM TABLE3)
 *     }
 * </pre>
 *
 * @param terms       size >= 1
 * @param ops         size == terms.size()-1
 * @param orderBy     OrderBy statement
 * @param limitOffset limit and offset definition.
 */
public record CompositeQueryImpl(List<Query> terms, List<SetOperator> ops, OrderBy orderBy, LimitOffset limitOffset) implements CompositeQuery {

    /**
     * This constructor validates that the terms size matches the number of operators.
     *
     * @param terms       size >= 1
     * @param ops         size == terms.size()-1
     * @param orderBy     OrderBy statement
     * @param limitOffset limit and offset definition.
     */
    public CompositeQueryImpl {
        if (ops.size() != terms.size() - 1) {
            throw new IllegalArgumentException("The number of operators should be 1 less then the number of terms: ops.size == terms.size()-1.");
        }
        terms = List.copyOf(terms);
        ops = List.copyOf(ops);
    }
}
