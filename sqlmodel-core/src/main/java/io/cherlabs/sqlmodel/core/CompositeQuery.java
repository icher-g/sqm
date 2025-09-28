package io.cherlabs.sqlmodel.core;

import java.util.List;
import java.util.Objects;

/**
 * Represents a composite query.
 * <p>Example for Union statement:</p>
 * <pre>
 *     {@code
 *     (SELECT * FROM TABLE1)
 *     UNION
 *     (SELECT * FROM TABLE2)
 *     }
 * </pre>
 */
public final class CompositeQuery extends Query<CompositeQuery> {

    private final List<? extends Query<?>> terms;   // size >= 1
    private final List<Op> ops;                     // size == terms.size()-1

    public CompositeQuery(List<? extends Query<?>> terms, List<Op> ops) {
        this.terms = Objects.requireNonNull(terms);
        this.ops = Objects.requireNonNull(ops);
        if (terms.isEmpty() || ops.size() != terms.size() - 1) {
            throw new IllegalArgumentException("CompositeQuery: operators must be terms.size()-1");
        }
    }

    private static UnsupportedOperationException unsupported(String what) {
        return new UnsupportedOperationException("CompositeQuery does not support " + what + " at top level; put it inside terms.");
    }

    public List<? extends Query<?>> terms() {
        return terms;
    }

    public List<Op> ops() {
        return ops;
    }

    @Override
    public CompositeQuery select(Column... cols) {
        throw unsupported("select");
    }

    /* ---- Disallow irrelevant mutators inherited from Query ---- */

    @Override
    public CompositeQuery select(List<Column> cols) {
        throw unsupported("select");
    }

    @Override
    public CompositeQuery from(Table t) {
        throw unsupported("from");
    }

    @Override
    public CompositeQuery where(Filter f) {
        throw unsupported("where");
    }

    @Override
    public CompositeQuery having(Filter f) {
        throw unsupported("having");
    }

    @Override
    public CompositeQuery join(Join... j) {
        throw unsupported("join");
    }

    @Override
    public CompositeQuery join(List<Join> j) {
        throw unsupported("join");
    }

    @Override
    public CompositeQuery groupBy(Group... g) {
        throw unsupported("groupBy");
    }

    @Override
    public CompositeQuery groupBy(List<Group> g) {
        throw unsupported("groupBy");
    }

    @Override
    public CompositeQuery distinct(boolean d) {
        throw unsupported("distinct");
    }

    @Override
    public List<Column> select() {
        return java.util.List.of();
    }

    @Override
    public List<Join> joins() {
        return java.util.List.of();
    }

    @Override
    public List<Group> groupBy() {
        return java.util.List.of();
    }

    public enum Kind {Union, Intersect, Except}

    public static final class Op {
        private final Kind kind;
        private final boolean all;

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

        public Kind kind() {
            return kind;
        }

        public boolean all() {
            return all;
        }
    }
}

