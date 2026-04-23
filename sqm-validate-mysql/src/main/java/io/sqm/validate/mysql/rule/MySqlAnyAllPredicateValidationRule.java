package io.sqm.validate.mysql.rule;

import io.sqm.core.AnyAllPredicate;
import io.sqm.core.Expression;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.rule.SchemaValidationRule;

/**
 * Validates MySQL support for quantified ANY/ALL predicate sources.
 */
public final class MySqlAnyAllPredicateValidationRule implements SchemaValidationRule<AnyAllPredicate> {

    /**
     * Creates a MySQL ANY/ALL predicate validation rule.
     */
    public MySqlAnyAllPredicateValidationRule() {
    }

    /**
     * Returns supported node type.
     *
     * @return ANY/ALL predicate type.
     */
    @Override
    public Class<AnyAllPredicate> nodeType() {
        return AnyAllPredicate.class;
    }

    /**
     * Validates that MySQL uses only query sources for quantified predicates.
     *
     * @param node ANY/ALL predicate node.
     * @param context schema validation context.
     */
    @Override
    public void validate(AnyAllPredicate node, SchemaValidationContext context) {
        if (node.source() instanceof Expression) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
                "MySQL does not support expression sources for ANY/ALL predicates",
                node,
                "predicate.any_all.source"
            );
        }
    }
}
