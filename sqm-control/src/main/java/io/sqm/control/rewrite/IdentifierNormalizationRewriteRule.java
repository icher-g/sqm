package io.sqm.control.rewrite;

import io.sqm.control.ExecutionContext;
import io.sqm.control.QueryRewriteResult;
import io.sqm.control.QueryRewriteRule;
import io.sqm.control.ReasonCode;
import io.sqm.control.BuiltInRewriteSettings;
import io.sqm.core.Query;
import io.sqm.core.transform.IdentifierNormalizationTransformer;

import java.util.Objects;

/**
 * Middleware rewrite rule that normalizes unquoted identifiers for deterministic output.
 *
 * <p>Quoted identifiers are preserved exactly.</p>
 */
public final class IdentifierNormalizationRewriteRule implements QueryRewriteRule {
    private static final String RULE_ID = "identifier-normalization";

    private final IdentifierNormalizationTransformer transformer;

    private IdentifierNormalizationRewriteRule(IdentifierNormalizationTransformer transformer) {
        this.transformer = transformer;
    }

    /**
     * Creates identifier normalization rewrite rule instance.
     *
     * @return rule instance
     */
    public static IdentifierNormalizationRewriteRule of() {
        return new IdentifierNormalizationRewriteRule(new IdentifierNormalizationTransformer());
    }

    /**
     * Creates identifier normalization rewrite rule instance using built-in rewrite settings.
     *
     * @param settings built-in rewrite settings
     * @return rule instance
     */
    public static IdentifierNormalizationRewriteRule of(BuiltInRewriteSettings settings) {
        Objects.requireNonNull(settings, "settings must not be null");
        return new IdentifierNormalizationRewriteRule(
            new IdentifierNormalizationTransformer(settings.identifierNormalizationCaseMode())
        );
    }

    @Override
    public String id() {
        return RULE_ID;
    }

    @Override
    public QueryRewriteResult apply(Query query, ExecutionContext context) {
        Objects.requireNonNull(query, "query must not be null");
        Objects.requireNonNull(context, "context must not be null");

        Query transformed = (Query) transformer.transform(query);
        if (transformed == query) {
            return QueryRewriteResult.unchanged(transformed);
        }
        return QueryRewriteResult.rewritten(transformed, id(), ReasonCode.REWRITE_IDENTIFIER_NORMALIZATION);
    }
}
