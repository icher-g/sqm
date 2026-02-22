package io.sqm.core.transform;

import io.sqm.core.CompositeQuery;
import io.sqm.core.Expression;
import io.sqm.core.LimitOffset;
import io.sqm.core.Node;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.core.SelectQueryBuilder;

/**
 * Injects a deterministic default LIMIT into queries when limit is absent.
 *
 * <p>Injection rules:</p>
 * <ul>
 *     <li>If query has no {@link LimitOffset}, inject default limit.</li>
 *     <li>If query has OFFSET without LIMIT, inject default limit and preserve OFFSET.</li>
 *     <li>If query has explicit LIMIT or {@code LIMIT ALL}, keep it unchanged.</li>
 * </ul>
 */
public final class LimitInjectionTransformer extends RecursiveNodeTransformer {
    private final Expression defaultLimit;

    private LimitInjectionTransformer(Expression defaultLimit) {
        this.defaultLimit = defaultLimit;
    }

    /**
     * Creates transformer with literal default limit.
     *
     * @param defaultLimit default limit value, must be greater than zero.
     * @return transformer instance.
     */
    public static LimitInjectionTransformer of(long defaultLimit) {
        if (defaultLimit <= 0) {
            throw new IllegalArgumentException("defaultLimit must be > 0");
        }
        return new LimitInjectionTransformer(Expression.literal(defaultLimit));
    }

    /**
     * Creates transformer with expression default limit.
     *
     * @param defaultLimitExpression default limit expression.
     * @return transformer instance.
     */
    public static LimitInjectionTransformer of(Expression defaultLimitExpression) {
        if (defaultLimitExpression == null) {
            throw new IllegalArgumentException("defaultLimitExpression must not be null");
        }
        return new LimitInjectionTransformer(defaultLimitExpression);
    }

    /**
     * Applies limit injection to a query tree.
     *
     * @param query query to transform.
     * @return transformed query or the original instance if unchanged.
     */
    public Query apply(Query query) {
        return (Query) transform(query);
    }

    @Override
    public Node visitSelectQuery(SelectQuery q) {
        var transformed = (SelectQuery) super.visitSelectQuery(q);
        var current = transformed.limitOffset();
        if (shouldNotInject(current)) {
            return transformed;
        }
        return SelectQueryBuilder.of(transformed)
            .limitOffset(LimitOffset.of(defaultLimit, current == null ? null : current.offset()))
            .build();
    }

    @Override
    public Node visitCompositeQuery(CompositeQuery q) {
        if (shouldNotInject(q.limitOffset())) {
            return q;
        }
        var transformed = (CompositeQuery) super.visitCompositeQuery(q);
        var current = transformed.limitOffset();
        return CompositeQuery.of(
            transformed.terms(),
            transformed.ops(),
            transformed.orderBy(),
            LimitOffset.of(defaultLimit, current == null ? null : current.offset())
        );
    }

    private static boolean shouldNotInject(LimitOffset limitOffset) {
        if (limitOffset == null) {
            return false;
        }
        if (limitOffset.limitAll()) {
            return true;
        }
        return limitOffset.limit() != null;
    }
}
