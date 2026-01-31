package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.math.BigDecimal;

/**
 * LIMIT/OFFSET pair (or OFFSET/FETCH mapped to these two).
 */
public non-sealed interface LimitOffset extends Node {
    /**
     * Initializes the class with provided limit.
     *
     * @param limit a limit to use.
     * @return new instance of {@link LimitOffset}.
     */
    static LimitOffset limit(long limit) {
        return limit(Expression.literal(limit));
    }

    /**
     * Initializes the class with provided limit expression.
     *
     * @param limit a limit expression.
     * @return new instance of {@link LimitOffset}.
     */
    static LimitOffset limit(Expression limit) {
        return new Impl(limit, null, false);
    }

    /**
     * Creates a {@link LimitOffset} representing {@code LIMIT ALL}.
     *
     * @return new instance of {@link LimitOffset}.
     */
    static LimitOffset all() {
        return new Impl(null, null, true);
    }

    /**
     * Creates a {@link LimitOffset} with the provided offset.
     *
     * @param offset an offset to use.
     * @return new instance of {@link LimitOffset}.
     */
    static LimitOffset offset(long offset) {
        return offset(Expression.literal(offset));
    }

    /**
     * Creates a {@link LimitOffset} with the provided offset expression.
     *
     * @param offset an offset expression.
     * @return new instance of {@link LimitOffset}.
     */
    static LimitOffset offset(Expression offset) {
        return new Impl(null, offset, false);
    }

    /**
     * Creates a {@link LimitOffset} with the provided limit and offset.
     *
     * @param limit  a limit to use.
     * @param offset an offset to use.
     * @return new instance of {@link LimitOffset}.
     */
    static LimitOffset of(Long limit, Long offset) {
        return new Impl(limit == null ? null : Expression.literal(limit), offset == null ? null : Expression.literal(offset), false);
    }

    /**
     * Creates a {@link LimitOffset} with the provided limit and offset expressions.
     *
     * @param limit  a limit expression.
     * @param offset an offset expression.
     * @return new instance of {@link LimitOffset}.
     */
    static LimitOffset of(Expression limit, Expression offset) {
        return new Impl(limit, offset, false);
    }

    /**
     * Creates a {@link LimitOffset} with the provided limit and offset expressions.
     *
     * @param limit    a limit expression.
     * @param offset   an offset expression.
     * @param limitAll {@code true} if the limit clause is {@code LIMIT ALL}.
     * @return new instance of {@link LimitOffset}.
     */
    static LimitOffset of(Expression limit, Expression offset, boolean limitAll) {
        return new Impl(limit, offset, limitAll);
    }

    /**
     * Gets a limit expression. null if absent.
     *
     * @return a limit.
     */
    Expression limit();

    /**
     * Indicates that the query has {@code LIMIT ALL}.
     *
     * @return {@code true} if limit is explicitly {@code ALL}.
     */
    boolean limitAll();

    /**
     * Gets an offset expression. null if absent.
     *
     * @return an offset.
     */
    Expression offset();

    /**
     * Accepts a {@link NodeVisitor} and dispatches control to the
     * visitor method corresponding to the concrete subtype
     *
     * @param v   the visitor instance to accept (must not be {@code null})
     * @param <R> the result type returned by the visitor
     * @return the result produced by the visitor
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitLimitOffset(this);
    }

    /**
     * LIMIT/OFFSET pair (or OFFSET/FETCH mapped to these two).
     *
     * @param limit  a limit.
     * @param offset an offset.
     * @param limitAll indicates that the query has {@code LIMIT ALL}.
     */
    record Impl(Expression limit, Expression offset, boolean limitAll) implements LimitOffset {

        /**
         * This constructor validates limit and offset are >= 0 for literal numbers.
         *
         * @param limit  a limit.
         * @param offset an offset.
         */
        public Impl {
            if (limitAll && limit != null) {
                throw new IllegalArgumentException("limitAll cannot be true when a limit expression is provided.");
            }
            if (limit instanceof LiteralExpr lit && lit.value() instanceof Number n && n.longValue() < 0) {
                throw new IllegalArgumentException("limit must be >= 0");
            }
            if (offset instanceof LiteralExpr lit && lit.value() instanceof Number n && n.longValue() < 0) {
                throw new IllegalArgumentException("offset must be >= 0");
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LimitOffset other)) return false;
            return limitAll == other.limitAll()
                && exprEquals(limit, other.limit())
                && exprEquals(offset, other.offset());
        }

        @Override
        public int hashCode() {
            int result = Boolean.hashCode(limitAll);
            result = 31 * result + exprHash(limit);
            result = 31 * result + exprHash(offset);
            return result;
        }

        private static boolean exprEquals(Expression left, Expression right) {
            if (left == right) return true;
            if (left == null || right == null) return false;
            if (left instanceof LiteralExpr l && right instanceof LiteralExpr r) {
                Object lv = l.value();
                Object rv = r.value();
                if (lv instanceof Number ln && rv instanceof Number rn) {
                    return numberEquals(ln, rn);
                }
            }
            return left.equals(right);
        }

        private static int exprHash(Expression expr) {
            if (expr == null) return 0;
            if (expr instanceof LiteralExpr l && l.value() instanceof Number n) {
                return numberHash(n);
            }
            return expr.hashCode();
        }

        private static boolean numberEquals(Number left, Number right) {
            return normalizeNumber(left).compareTo(normalizeNumber(right)) == 0;
        }

        private static int numberHash(Number value) {
            return normalizeNumber(value).stripTrailingZeros().hashCode();
        }

        private static BigDecimal normalizeNumber(Number value) {
            return new BigDecimal(value.toString());
        }
    }
}
