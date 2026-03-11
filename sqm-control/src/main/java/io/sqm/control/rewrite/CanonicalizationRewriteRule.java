package io.sqm.control.rewrite;

import io.sqm.control.decision.ReasonCode;
import io.sqm.control.execution.ExecutionContext;
import io.sqm.control.pipeline.StatementRewriteResult;
import io.sqm.control.pipeline.StatementRewriteRule;
import io.sqm.core.Statement;
import io.sqm.core.transform.ArithmeticSimplifier;
import io.sqm.core.transform.BooleanPredicateSimplifier;

import java.util.Objects;

/**
 * Middleware rewrite rule that applies deterministic AST canonicalization transforms.
 *
 * <p>Current implementation composes safe local canonicalizers including
 * arithmetic and boolean-predicate simplification.</p>
 */
public final class CanonicalizationRewriteRule implements StatementRewriteRule {
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
     * @param statement parsed statement model
     * @param context execution context
     * @return rewrite result
     */
    @Override
    public StatementRewriteResult apply(Statement statement, ExecutionContext context) {
        Objects.requireNonNull(statement, "statement must not be null");
        Objects.requireNonNull(context, "context must not be null");

        Statement transformed = (Statement) arithmeticSimplifier.transform(statement);
        transformed = (Statement) booleanTransformer.transform(transformed);
        if (transformed == statement) {
            return StatementRewriteResult.unchanged(transformed);
        }
        return StatementRewriteResult.rewritten(transformed, id(), ReasonCode.REWRITE_CANONICALIZATION);
    }
}




