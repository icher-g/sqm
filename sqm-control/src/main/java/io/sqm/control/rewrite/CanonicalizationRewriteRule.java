package io.sqm.control.rewrite;

import io.sqm.control.ExecutionContext;
import io.sqm.control.QueryRewriteResult;
import io.sqm.control.QueryRewriteRule;
import io.sqm.control.ReasonCode;
import io.sqm.core.Query;
import io.sqm.core.transform.ArithmeticSimplifier;

import java.util.Objects;

/**
 * Middleware rewrite rule that applies deterministic AST canonicalization transforms.
 *
 * <p>Current implementation uses {@link ArithmeticSimplifier} for safe arithmetic normalization.</p>
 */
public final class CanonicalizationRewriteRule implements QueryRewriteRule {
    private static final String RULE_ID = "canonicalization";

    private final ArithmeticSimplifier transformer;

    private CanonicalizationRewriteRule(ArithmeticSimplifier transformer) {
        this.transformer = transformer;
    }

    /**
     * Creates canonicalization rewrite rule instance.
     *
     * @return rule instance
     */
    public static CanonicalizationRewriteRule of() {
        return new CanonicalizationRewriteRule(new ArithmeticSimplifier());
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
     * Applies canonicalization and reports a rewrite only when the AST actually changes.
     *
     * @param query parsed query model
     * @param context execution context
     * @return rewrite result
     */
    @Override
    public QueryRewriteResult apply(Query query, ExecutionContext context) {
        Objects.requireNonNull(query, "query must not be null");
        Objects.requireNonNull(context, "context must not be null");

        Query transformed = (Query) transformer.transform(query);
        if (transformed == query) {
            return QueryRewriteResult.unchanged(transformed);
        }
        return QueryRewriteResult.rewritten(transformed, id(), ReasonCode.REWRITE_CANONICALIZATION);
    }
}
