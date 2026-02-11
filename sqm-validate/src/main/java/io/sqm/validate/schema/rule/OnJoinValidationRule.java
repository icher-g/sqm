package io.sqm.validate.schema.rule;

import io.sqm.core.ColumnExpr;
import io.sqm.core.OnJoin;
import io.sqm.core.UnaryPredicate;
import io.sqm.core.walk.RecursiveNodeVisitor;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.model.DbType;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Validates ON-join predicate presence and join-side alias visibility.
 */
final class OnJoinValidationRule implements SchemaValidationRule<OnJoin> {
    /**
     * Returns supported node type.
     *
     * @return ON join type.
     */
    @Override
    public Class<OnJoin> nodeType() {
        return OnJoin.class;
    }

    /**
     * Validates ON predicate constraints for a regular join.
     *
     * @param node ON join node.
     * @param context schema validation context.
     */
    @Override
    public void validate(OnJoin node, SchemaValidationContext context) {
        if (node.on() == null) {
            context.addProblem(
                ValidationProblem.Code.JOIN_ON_MISSING_PREDICATE,
                "JOIN ON predicate is required for regular joins",
                node,
                "join.on"
            );
            return;
        }
        validateVisibleAliases(node, context);
        validateBooleanSemantics(node, context);
    }

    /**
     * Validates that ON predicate does not reference aliases from later joins.
     *
     * @param node ON join node.
     * @param context schema validation context.
     */
    private static void validateVisibleAliases(OnJoin node, SchemaValidationContext context) {
        var allowedAliases = context.onJoinVisibleAliases(node);
        var currentScopeAliases = Set.copyOf(context.currentScopeSourceKeys());
        for (var alias : collectQualifiedAliases(node)) {
            if (!currentScopeAliases.contains(alias)) {
                // Let generic column resolver report unknown aliases.
                continue;
            }
            if (allowedAliases.contains(alias)) {
                continue;
            }
            context.addProblem(
                ValidationProblem.Code.JOIN_ON_INVALID_REFERENCE,
                "JOIN ON cannot reference alias from later join: " + alias,
                node,
                "join.on"
            );
        }
    }

    /**
     * Collects normalized qualified aliases referenced in ON predicate.
     *
     * @param node ON join node.
     * @return normalized alias set.
     */
    private static Set<String> collectQualifiedAliases(OnJoin node) {
        var collector = new QualifiedAliasCollector();
        node.on().accept(collector);
        return collector.aliases();
    }

    /**
     * Validates ON-predicate boolean expression constraints.
     *
     * @param node ON join node.
     * @param context schema validation context.
     */
    private static void validateBooleanSemantics(OnJoin node, SchemaValidationContext context) {
        node.on().accept(new OnBooleanSemanticsVisitor(node, context));
    }

    /**
     * Predicate visitor that captures qualified alias references.
     */
    private static final class QualifiedAliasCollector extends RecursiveNodeVisitor<Void> {
        private final Set<String> aliases = new LinkedHashSet<>();

        /**
         * Returns collected normalized aliases.
         *
         * @return normalized alias set.
         */
        private Set<String> aliases() {
            return Set.copyOf(aliases);
        }

        @Override
        protected Void defaultResult() {
            return null;
        }

        @Override
        public Void visitColumnExpr(ColumnExpr c) {
            if (c.tableAlias() != null) {
                aliases.add(c.tableAlias().toLowerCase(Locale.ROOT));
            }
            return super.visitColumnExpr(c);
        }
    }

    /**
     * Predicate visitor that validates boolean-typed unary predicate expressions in JOIN ON.
     */
    private static final class OnBooleanSemanticsVisitor extends RecursiveNodeVisitor<Void> {
        private final OnJoin source;
        private final SchemaValidationContext context;

        private OnBooleanSemanticsVisitor(OnJoin source, SchemaValidationContext context) {
            this.source = source;
            this.context = context;
        }

        @Override
        protected Void defaultResult() {
            return null;
        }

        @Override
        public Void visitUnaryPredicate(UnaryPredicate p) {
            var inferred = context.inferType(p.expr());
            if (inferred.isPresent() && DbType.isKnown(inferred.get()) && inferred.get() != DbType.BOOLEAN) {
                context.addProblem(
                    ValidationProblem.Code.JOIN_ON_INVALID_BOOLEAN_EXPRESSION,
                    "JOIN ON unary predicate expression must be BOOLEAN but was " + inferred.get(),
                    source,
                    "join.on"
                );
            }
            return super.visitUnaryPredicate(p);
        }
    }
}
