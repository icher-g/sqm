package io.sqm.validate.schema.rule;

import io.sqm.core.FunctionExpr;
import io.sqm.core.OverSpec;
import io.sqm.core.SelectQuery;
import io.sqm.core.walk.RecursiveNodeVisitor;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Validates that window references used by OVER clauses are defined in the current SELECT.
 */
final class WindowReferenceValidationRule implements SchemaValidationRule<SelectQuery> {
    /**
     * Returns supported node type.
     *
     * @return select query type.
     */
    @Override
    public Class<SelectQuery> nodeType() {
        return SelectQuery.class;
    }

    /**
     * Validates window references in all function OVER clauses for the provided SELECT.
     *
     * @param node select query node.
     * @param context schema validation context.
     */
    @Override
    public void validate(SelectQuery node, SchemaValidationContext context) {
        var windowNames = collectWindowNames(node);
        node.accept(new WindowReferenceVisitor(node, context, windowNames));
    }

    /**
     * Collects normalized window names declared in WINDOW clause.
     *
     * @param node select query.
     * @return normalized window names.
     */
    private static Set<String> collectWindowNames(SelectQuery node) {
        var names = new HashSet<String>(node.windows().size());
        for (var window : node.windows()) {
            if (window.name() != null) {
                names.add(normalize(window.name()));
            }
        }
        return names;
    }

    /**
     * Normalizes identifier for case-insensitive comparison.
     *
     * @param identifier identifier value.
     * @return normalized identifier.
     */
    private static String normalize(String identifier) {
        return identifier.toLowerCase(Locale.ROOT);
    }

    /**
     * Local visitor that validates OVER references while skipping nested SELECT blocks.
     */
    private static final class WindowReferenceVisitor extends RecursiveNodeVisitor<Void> {
        private final SelectQuery root;
        private final SchemaValidationContext context;
        private final Set<String> windowNames;

        /**
         * Creates validation visitor for one SELECT scope.
         *
         * @param root root select query.
         * @param context validation context.
         * @param windowNames normalized window names visible in root SELECT.
         */
        private WindowReferenceVisitor(
            SelectQuery root,
            SchemaValidationContext context,
            Set<String> windowNames
        ) {
            this.root = root;
            this.context = context;
            this.windowNames = windowNames;
        }

        /**
         * Returns default visitor result.
         *
         * @return {@code null}.
         */
        @Override
        protected Void defaultResult() {
            return null;
        }

        /**
         * Visits select query and avoids traversing nested SELECT blocks.
         *
         * @param query select query node.
         * @return default result.
         */
        @Override
        public Void visitSelectQuery(SelectQuery query) {
            if (query != root) {
                return defaultResult();
            }
            return super.visitSelectQuery(query);
        }

        /**
         * Validates function OVER reference against known window names.
         *
         * @param function function expression.
         * @return default result.
         */
        @Override
        public Void visitFunctionExpr(FunctionExpr function) {
            super.visitFunctionExpr(function);
            validateOver(function.over(), function);
            return defaultResult();
        }

        /**
         * Validates that OVER references point to declared WINDOW names.
         *
         * @param over OVER spec to validate.
         * @param source source node for diagnostics.
         */
        private void validateOver(OverSpec over, FunctionExpr source) {
            if (over == null) {
                return;
            }
            switch (over) {
                case OverSpec.Ref ref -> validateWindowName(ref.windowName(), source);
                case OverSpec.Def def -> {
                    if (def.baseWindow() != null) {
                        validateWindowName(def.baseWindow(), source);
                    }
                }
                default -> {
                }
            }
        }

        /**
         * Validates single window name existence.
         *
         * @param windowName referenced window name.
         * @param source source node for diagnostics.
         */
        private void validateWindowName(String windowName, FunctionExpr source) {
            if (windowName == null || windowNames.contains(normalize(windowName))) {
                return;
            }
            context.addProblem(
                ValidationProblem.Code.WINDOW_NOT_FOUND,
                "Window not found: " + windowName,
                source,
                "window.reference"
            );
        }
    }
}
