package io.sqm.control.rewrite;

import io.sqm.control.decision.ReasonCode;
import io.sqm.control.execution.ExecutionContext;
import io.sqm.control.pipeline.StatementRewriteResult;
import io.sqm.control.pipeline.StatementRewriteRule;
import io.sqm.core.Statement;
import io.sqm.core.transform.IdentifierNormalizationTransformer;

import java.util.Objects;

/**
 * Middleware rewrite rule that normalizes unquoted identifiers for deterministic result.
 *
 * <p>Quoted identifiers are preserved exactly.</p>
 */
public final class IdentifierNormalizationRewriteRule implements StatementRewriteRule {
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
    public StatementRewriteResult apply(Statement statement, ExecutionContext context) {
        Objects.requireNonNull(statement, "statement must not be null");
        Objects.requireNonNull(context, "context must not be null");

        Statement transformed = (Statement) transformer.transform(statement);
        if (transformed == statement) {
            return StatementRewriteResult.unchanged(transformed);
        }
        return StatementRewriteResult.rewritten(transformed, id(), ReasonCode.REWRITE_IDENTIFIER_NORMALIZATION);
    }
}




