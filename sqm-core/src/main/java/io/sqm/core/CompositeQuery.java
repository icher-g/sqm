package io.sqm.core;

import io.sqm.core.traits.HasLimit;
import io.sqm.core.traits.HasOffset;
import io.sqm.core.traits.HasOrderBy;
import io.sqm.core.traits.HasTerms;

import java.util.List;
import java.util.Objects;

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
public record CompositeQuery(List<Query> terms, List<Op> ops, OrderBy orderBy, LimitOffset limitOffset) implements Query, HasTerms, HasLimit, HasOffset, HasOrderBy {

    public CompositeQuery(List<Query> terms, List<Op> ops) {
        this(terms, ops, null, null);
    }

    public CompositeQuery(List<Query> terms, List<Op> ops, OrderBy orderBy, LimitOffset limitOffset) {
        this.terms = Objects.requireNonNull(terms);
        this.ops = Objects.requireNonNull(ops);
        this.orderBy = orderBy;
        this.limitOffset = limitOffset;
        if (terms.isEmpty() || ops.size() != terms.size() - 1) {
            throw new IllegalArgumentException("CompositeQuery: operators must be terms.size()-1");
        }
    }

    @Override
    public Long limit() {
        return limitOffset == null ? null : limitOffset.limit();
    }

    @Override
    public Long offset() {
        return limitOffset == null ? null : limitOffset.offset();
    }

    /**
     * Adds an OrderBy statement to the composite query.
     *
     * @param items a list of items in the OrderBy statement.
     * @return A new instance of the composite query with the provided OrderBy items. All the rest of the fields are preserved.
     */
    public CompositeQuery orderBy(List<Order> items) {
        return new CompositeQuery(terms, ops, new OrderBy(items), limitOffset);
    }

    /**
     * Adds an OrderBy statement to the composite query.
     *
     * @param items a list of items in the OrderBy statement.
     * @return A new instance of the composite query with the provided OrderBy items. All the rest of the fields are preserved.
     */
    public CompositeQuery orderBy(Order... items) {
        return new CompositeQuery(terms, ops, new OrderBy(List.of(items)), limitOffset);
    }

    /**
     * Adds a limit to the composite query.
     *
     * @param limit a limit to add to the OrderBy statement.
     * @return A new instance of the composite query with the provided limit. All the rest of the fields are preserved.
     */
    public CompositeQuery limit(Long limit) {
        return new CompositeQuery(terms, ops, orderBy, LimitOffset.of(limit, offset()));
    }

    /**
     * Adds an offset to the composite query.
     *
     * @param offset an offset to add to the OrderBy statement.
     * @return A new instance of the composite query with the provided offset. All the rest of the fields are preserved.
     */
    public CompositeQuery offset(Long offset) {
        return new CompositeQuery(terms, ops, orderBy, LimitOffset.of(limit(), offset));
    }

    public enum Kind {
        Union,
        Intersect,
        Except
    }

    public record Op(Kind kind, boolean all) {
        public Op(Kind kind, boolean all) {
            this.kind = Objects.requireNonNull(kind, "kind");
            this.all = all;
        }

        public static Op union() {
            return new Op(Kind.Union, false);
        }

        public static Op unionAll() {
            return new Op(Kind.Union, true);
        }

        public static Op intersect() {
            return new Op(Kind.Intersect, false);
        }

        public static Op intersectAll() {
            return new Op(Kind.Intersect, true);
        }

        public static Op except() {
            return new Op(Kind.Except, false);
        }

        public static Op exceptAll() {
            return new Op(Kind.Except, true);
        }
    }
}

