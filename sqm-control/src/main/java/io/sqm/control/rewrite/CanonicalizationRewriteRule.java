package io.sqm.control.rewrite;

import io.sqm.control.ExecutionContext;
import io.sqm.control.QueryRewriteResult;
import io.sqm.control.QueryRewriteRule;
import io.sqm.control.ReasonCode;
import io.sqm.core.Query;
import io.sqm.core.transform.ArithmeticSimplifier;
import io.sqm.core.transform.BooleanPredicateSimplifier;

import java.util.Objects;

/**
 * Middleware rewrite rule that applies deterministic AST canonicalization transforms.
 *
 * <p>Current implementation composes safe local canonicalizers including
 * arithmetic and boolean-predicate simplification.</p>
 */
public final class CanonicalizationRewriteRule implements QueryRewriteRule {
    private static final String RULE_ID = "canonicalization";

    private final ArithmeticSimplifier arithmeticSimplifier;
    private final BooleanPredicateSimplifier booleanTransformer;

    private CanonicalizationRewriteRule(
        ArithmeticSimplifier arithmeticSimplifier,
        BooleanPredicateSimplifier booleanTransformer
    ) {
        this.arithmeticSimplifier = arithmeticSimplifier;
        this.booleanTransformer = booleanTransformer;
    }

    /**
     * Creates canonicalization rewrite rule instance.
     *
     * @return rule instance
     */
    public static CanonicalizationRewriteRule of() {
        return new CanonicalizationRewriteRule(
            new ArithmeticSimplifier(),
            new BooleanPredicateSimplifier()
        );
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

        Query transformed = (Query) arithmeticSimplifier.transform(query);
        transformed = (Query) booleanTransformer.transform(transformed);
        if (transformed == query) {
            return QueryRewriteResult.unchanged(transformed);
        }
        return QueryRewriteResult.rewritten(transformed, id(), ReasonCode.REWRITE_CANONICALIZATION);
    }
}
