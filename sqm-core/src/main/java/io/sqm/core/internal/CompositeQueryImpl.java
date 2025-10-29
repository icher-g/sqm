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
 * @param terms size >= 1
 * @param ops   size == terms.size()-1
 */
public record CompositeQueryImpl(List<Query> terms, List<SetOperator> ops, OrderBy orderBy, LimitOffset limitOffset) implements CompositeQuery {

    public CompositeQueryImpl {
        if (ops.size() != terms.size() - 1) {
            throw new IllegalArgumentException("The number of operators should be 1 less then the number of terms: ops.size == terms.size()-1.");
        }

        // Validate ANSI constraints on each term (no per-term ORDER/LIMIT/OFFSET)
        for (int i = 0; i < terms.size(); i++) {
            if (terms.get(i) instanceof SelectQuery t) {
                if (t.limit() != null || t.offset() != null || (t.orderBy() != null && !t.orderBy().items().isEmpty())) {
                    throw new UnsupportedOperationException(
                        "ANSI: operands of UNION/INTERSECT/EXCEPT must not have their own ORDER BY / LIMIT / OFFSET (term #" + (i + 1) + ")"
                    );
                }
            }
        }
    }
}
