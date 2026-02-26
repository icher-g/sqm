package io.sqm.control.rewrite;

import io.sqm.control.*;
import io.sqm.core.CompositeQuery;
import io.sqm.core.Expression;
import io.sqm.core.LimitOffset;
import io.sqm.core.LiteralExpr;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.core.SelectQueryBuilder;
import io.sqm.core.WithQuery;
import io.sqm.core.transform.LimitInjectionTransformer;

import java.util.Objects;

/**
 * Middleware rewrite rule that injects a default LIMIT using SQM core transformer support.
 */
public final class LimitInjectionRewriteRule implements QueryRewriteRule {
    private static final String RULE_ID = "limit-injection";

    private final LimitInjectionTransformer transformer;
    private final BuiltInRewriteSettings settings;

    private LimitInjectionRewriteRule(LimitInjectionTransformer transformer, BuiltInRewriteSettings settings) {
        this.transformer = transformer;
        this.settings = settings;
    }

    /**
     * Creates a limit-injection rewrite rule.
     *
     * @param defaultLimit default LIMIT value to inject when absent; must be greater than zero
     * @return rule instance
     */
    public static LimitInjectionRewriteRule of(long defaultLimit) {
        BuiltInRewriteSettings settings = BuiltInRewriteSettings.builder()
            .defaultLimitInjectionValue(defaultLimit)
            .build();
        return new LimitInjectionRewriteRule(LimitInjectionTransformer.of(defaultLimit), settings);
    }

    /**
     * Creates a limit-injection rewrite rule with explicit LIMIT policy settings.
     *
     * @param settings built-in rewrite settings used for limit injection and max-limit behavior
     * @return rule instance
     */
    public static LimitInjectionRewriteRule of(BuiltInRewriteSettings settings) {
        Objects.requireNonNull(settings, "settings must not be null");
        long defaultLimit = settings.maxAllowedLimit() != null
            && settings.limitExcessMode() == LimitExcessMode.CLAMP
            ? Math.min(settings.defaultLimitInjectionValue(), settings.maxAllowedLimit())
            : settings.defaultLimitInjectionValue();
        return new LimitInjectionRewriteRule(LimitInjectionTransformer.of(defaultLimit), settings);
    }

    /**
     * Returns a stable rule identifier.
     *
     * @return rule identifier
     */
    @Override
    public String id() {
        return RULE_ID;
    }

    /**
     * Applies limit injection and reports a rewrite only when the AST actually changes.
     *
     * @param query parsed query model
     * @param context execution context
     * @return rewrite result
     */
    @Override
    public QueryRewriteResult apply(Query query, ExecutionContext context) {
        Objects.requireNonNull(query, "query must not be null");
        Objects.requireNonNull(context, "context must not be null");

        Query transformed = transformer.apply(query);
        transformed = enforceMaxLimitPolicy(transformed);
        if (transformed == query) {
            return QueryRewriteResult.unchanged(transformed);
        }
        return QueryRewriteResult.rewritten(transformed, id(), ReasonCode.REWRITE_LIMIT);
    }

    private Query enforceMaxLimitPolicy(Query query) {
        if (settings.maxAllowedLimit() == null) {
            return query;
        }
        return switch (query) {
            case SelectQuery select -> enforceSelectLimit(select);
            case CompositeQuery composite -> enforceCompositeLimit(composite);
            case WithQuery with -> enforceWithBodyLimit(with);
            default -> query;
        };
    }

    private Query enforceWithBodyLimit(WithQuery with) {
        if (with.body() == null) {
            return with;
        }
        Query body = enforceMaxLimitPolicy(with.body());
        return body == with.body() ? with : with.body(body);
    }

    private Query enforceSelectLimit(SelectQuery select) {
        LimitOffset current = select.limitOffset();
        if (current == null) {
            return select;
        }
        if (current.limitAll()) {
            return handleExceededLimit(select, current, Long.MAX_VALUE);
        }
        Long currentLimit = numericLiteral(current.limit());
        if (current.limit() == null) {
            return select;
        }
        if (currentLimit == null) {
            throw new RewriteDenyException(
                ReasonCode.DENY_MAX_ROWS,
                "Query LIMIT must be a literal value <= %d".formatted(settings.maxAllowedLimit())
            );
        }
        if (currentLimit <= settings.maxAllowedLimit()) {
            return select;
        }
        return handleExceededLimit(select, current, currentLimit);
    }

    private Query enforceCompositeLimit(CompositeQuery composite) {
        LimitOffset current = composite.limitOffset();
        if (current == null) {
            return composite;
        }
        if (current.limitAll()) {
            return handleExceededLimit(composite, current, Long.MAX_VALUE);
        }
        Long currentLimit = numericLiteral(current.limit());
        if (current.limit() == null) {
            return composite;
        }
        if (currentLimit == null) {
            throw new RewriteDenyException(
                ReasonCode.DENY_MAX_ROWS,
                "Query LIMIT must be a literal value <= %d".formatted(settings.maxAllowedLimit())
            );
        }
        if (currentLimit <= settings.maxAllowedLimit()) {
            return composite;
        }
        return handleExceededLimit(composite, current, currentLimit);
    }

    private Query handleExceededLimit(Query query, LimitOffset current, long currentLimit) {
        if (settings.limitExcessMode() == LimitExcessMode.DENY) {
            throw new RewriteDenyException(
                ReasonCode.DENY_MAX_ROWS,
                "Query LIMIT %s exceeds configured max %d".formatted(printableLimit(currentLimit), settings.maxAllowedLimit())
            );
        }
        Expression clamped = Expression.literal(settings.maxAllowedLimit().longValue());
        return switch (query) {
            case SelectQuery select -> SelectQueryBuilder.of(select)
                .limitOffset(LimitOffset.of(clamped, current.offset()))
                .build();
            case CompositeQuery composite -> CompositeQuery.of(
                composite.terms(),
                composite.ops(),
                composite.orderBy(),
                LimitOffset.of(clamped, current.offset())
            );
            default -> query;
        };
    }

    private static String printableLimit(long limit) {
        return limit == Long.MAX_VALUE ? "ALL" : Long.toString(limit);
    }

    private static Long numericLiteral(Expression expression) {
        if (expression instanceof LiteralExpr literalExpr && literalExpr.value() instanceof Number n) {
            return n.longValue();
        }
        return null;
    }
}
