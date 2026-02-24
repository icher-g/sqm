package io.sqm.validate.schema.rule;

import io.sqm.core.CompositeQuery;
import io.sqm.core.Query;
import io.sqm.core.SetOperator;
import io.sqm.core.Table;
import io.sqm.core.WithQuery;
import io.sqm.core.walk.RecursiveNodeVisitor;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.catalog.model.CatalogType;
import io.sqm.validate.schema.model.CatalogTypeSemantics;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Validates top-level WITH clause definitions.
 */
final class WithQueryValidationRule implements SchemaValidationRule<WithQuery> {
    private final ProjectionShapeInspector projectionShapeInspector;

    /**
     * Creates WITH-query validation rule.
     *
     * @param projectionShapeInspector projection shape inspector.
     */
    WithQueryValidationRule(ProjectionShapeInspector projectionShapeInspector) {
        this.projectionShapeInspector = projectionShapeInspector;
    }

    /**
     * Normalizes CTE names for case-insensitive matching.
     *
     * @param value raw CTE name.
     * @return normalized key.
     */
    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    /**
     * Returns supported node type.
     *
     * @return with query type.
     */
    @Override
    public Class<WithQuery> nodeType() {
        return WithQuery.class;
    }

    /**
     * Validates CTE name uniqueness and recursion safety inside one WITH block.
     *
     * @param node with query node.
     * @param context schema validation context.
     */
    @Override
    public void validate(WithQuery node, SchemaValidationContext context) {
        var seen = new HashSet<String>(node.ctes().size());
        for (var cte : node.ctes()) {
            if (cte == null || cte.name() == null) {
                continue;
            }
            var normalized = normalize(cte.name().value());
            if (seen.add(normalized)) {
                continue;
            }
            context.addProblem(
                ValidationProblem.Code.DUPLICATE_CTE_NAME,
                "Duplicate CTE name in WITH block: " + cte.name().value(),
                cte,
                "with.cte"
            );
        }
        validateNonRecursiveSelfReference(node, context);
        validateRecursiveCteStructure(node, context);
    }

    /**
     * Validates that non-recursive WITH does not contain direct self-references.
     *
     * @param node with query node.
     * @param context schema validation context.
     */
    private static void validateNonRecursiveSelfReference(WithQuery node, SchemaValidationContext context) {
        if (node.recursive()) {
            return;
        }
        for (var cte : node.ctes()) {
            if (cte == null || cte.name() == null || cte.body() == null) {
                continue;
            }
            if (!containsUnqualifiedTableReference(cte.body(), cte.name().value())) {
                continue;
            }
            context.addProblem(
                ValidationProblem.Code.CTE_RECURSION_NOT_ALLOWED,
                "CTE '" + cte.name().value() + "' references itself but WITH is not recursive",
                cte,
                "with.cte"
            );
        }
    }

    /**
     * Validates recursive CTE structural requirements.
     *
     * @param node with query node.
     * @param context schema validation context.
     */
    private void validateRecursiveCteStructure(WithQuery node, SchemaValidationContext context) {
        if (!node.recursive()) {
            return;
        }
        for (var cte : node.ctes()) {
            if (cte == null || cte.name() == null || cte.body() == null) {
                continue;
            }
            if (!containsUnqualifiedTableReference(cte.body(), cte.name().value())) {
                continue;
            }
            validateRecursiveCte(cte, context);
        }
    }

    /**
     * Validates one recursive CTE.
     *
     * @param cte recursive CTE.
     * @param context schema validation context.
     */
    private void validateRecursiveCte(io.sqm.core.CteDef cte, SchemaValidationContext context) {
        if (!(cte.body() instanceof CompositeQuery composite) || composite.terms().size() < 2) {
            reportRecursiveStructure(
                cte,
                "Recursive CTE '" + cte.name().value() + "' must use a set operation with anchor and recursive terms",
                context
            );
            return;
        }
        if (!composite.ops().stream().allMatch(WithQueryValidationRule::isRecursiveSetOperator)) {
            reportRecursiveStructure(
                cte,
                "Recursive CTE '" + cte.name().value() + "' must use UNION or UNION ALL between terms",
                context
            );
            return;
        }

        var anchor = composite.terms().getFirst();
        if (containsUnqualifiedTableReference(anchor, cte.name().value())) {
            reportRecursiveStructure(
                cte,
                "Recursive CTE '" + cte.name().value() + "' anchor term must not reference itself",
                context
            );
        }

        var recursiveTermFound = false;
        for (int i = 1; i < composite.terms().size(); i++) {
            var term = composite.terms().get(i);
            if (containsUnqualifiedTableReference(term, cte.name().value())) {
                recursiveTermFound = true;
            }
        }
        if (!recursiveTermFound) {
            reportRecursiveStructure(
                cte,
                "Recursive CTE '" + cte.name().value() + "' must reference itself in at least one recursive term",
                context
            );
        }
        validateRecursiveTermProjectionArity(cte, composite, context);
        validateRecursiveTermProjectionTypes(cte, composite, context);
    }

    /**
     * Validates projection arity compatibility between recursive CTE terms.
     *
     * @param cte recursive CTE.
     * @param composite recursive CTE body.
     * @param context schema validation context.
     */
    private void validateRecursiveTermProjectionArity(
        io.sqm.core.CteDef cte,
        CompositeQuery composite,
        SchemaValidationContext context
    ) {
        var anchorArity = projectionShapeInspector.projectionArity(composite.terms().getFirst());
        if (anchorArity.isEmpty()) {
            return;
        }
        for (int i = 1; i < composite.terms().size(); i++) {
            var termArity = projectionShapeInspector.projectionArity(composite.terms().get(i));
            if (termArity.isEmpty()) {
                continue;
            }
            if (termArity.get().equals(anchorArity.get())) {
                continue;
            }
            reportRecursiveStructure(
                cte,
                "Recursive CTE '" + cte.name().value() + "' term "
                    + (i + 1)
                    + " projection width "
                    + termArity.get()
                    + " does not match anchor width "
                    + anchorArity.get(),
                context
            );
        }
    }

    /**
     * Validates projection type compatibility between anchor and recursive terms.
     *
     * @param cte recursive CTE.
     * @param composite recursive CTE body.
     * @param context schema validation context.
     */
    private void validateRecursiveTermProjectionTypes(
        io.sqm.core.CteDef cte,
        CompositeQuery composite,
        SchemaValidationContext context
    ) {
        var anchorTypes = projectionShapeInspector.expressionProjectionTypes(composite.terms().getFirst(), context);
        if (anchorTypes.isEmpty()) {
            return;
        }
        for (int i = 1; i < composite.terms().size(); i++) {
            var termTypes = projectionShapeInspector.expressionProjectionTypes(composite.terms().get(i), context);
            if (termTypes.isEmpty()) {
                continue;
            }
            validateRecursiveTypePair(cte, i, anchorTypes.get(), termTypes.get(), context);
        }
    }

    /**
     * Validates one anchor/term projection type pair.
     *
     * @param cte recursive CTE.
     * @param termIndex term index within composite query.
     * @param anchorTypes anchor projection types.
     * @param termTypes recursive term projection types.
     * @param context schema validation context.
     */
    private static void validateRecursiveTypePair(
        io.sqm.core.CteDef cte,
        int termIndex,
        List<Optional<CatalogType>> anchorTypes,
        List<Optional<CatalogType>> termTypes,
        SchemaValidationContext context
    ) {
        var width = Math.min(anchorTypes.size(), termTypes.size());
        for (int c = 0; c < width; c++) {
            if (anchorTypes.get(c).isEmpty() || termTypes.get(c).isEmpty()) {
                continue;
            }
            var anchorType = anchorTypes.get(c).get();
            var termType = termTypes.get(c).get();
            if (CatalogTypeSemantics.comparable(anchorType, termType)) {
                continue;
            }
            context.addProblem(
                ValidationProblem.Code.CTE_RECURSIVE_TYPE_MISMATCH,
                "Recursive CTE '" + cte.name().value() + "' term "
                    + (termIndex + 1)
                    + ", column "
                    + (c + 1)
                    + " has incompatible type "
                    + termType
                    + " (anchor type: "
                    + anchorType
                    + ")",
                cte,
                "with.cte"
            );
        }
    }

    /**
     * Reports recursive CTE structure problem.
     *
     * @param cte CTE node.
     * @param message diagnostic message.
     * @param context validation context.
     */
    private static void reportRecursiveStructure(
        io.sqm.core.CteDef cte,
        String message,
        SchemaValidationContext context
    ) {
        context.addProblem(
            ValidationProblem.Code.CTE_RECURSIVE_STRUCTURE_INVALID,
            message,
            cte,
            "with.cte"
        );
    }

    /**
     * Returns whether set operator is valid for recursive CTE composition.
     *
     * @param op set operator.
     * @return true when recursive-compatible.
     */
    private static boolean isRecursiveSetOperator(SetOperator op) {
        return op == SetOperator.UNION || op == SetOperator.UNION_ALL;
    }

    /**
     * Checks whether query contains an unqualified table reference matching target CTE name.
     *
     * <p>Nested WITH blocks are intentionally skipped to avoid false positives due to
     * inner-scope shadowing.</p>
     *
     * @param query query to inspect.
     * @param cteName CTE name.
     * @return true when matching table reference is found.
     */
    private static boolean containsUnqualifiedTableReference(Query query, String cteName) {
        var visitor = new CteReferenceVisitor(normalize(cteName));
        query.accept(visitor);
        return visitor.found();
    }

    /**
     * Visitor that detects one matching unqualified table reference.
     */
    private static final class CteReferenceVisitor extends RecursiveNodeVisitor<Void> {
        private final String targetCteName;
        private boolean found;

        /**
         * Creates reference detector.
         *
         * @param targetCteName normalized CTE name to find.
         */
        private CteReferenceVisitor(String targetCteName) {
            this.targetCteName = targetCteName;
        }

        /**
         * Indicates whether reference was found.
         *
         * @return true when matched.
         */
        private boolean found() {
            return found;
        }

        @Override
        protected Void defaultResult() {
            return null;
        }

        /**
         * Stops traversal for nested WITH blocks.
         *
         * @param q nested with query.
         * @return default result.
         */
        @Override
        public Void visitWithQuery(WithQuery q) {
            return defaultResult();
        }

        /**
         * Detects unqualified table reference matching target CTE name.
         *
         * @param t table node.
         * @return default result.
         */
        @Override
        public Void visitTable(Table t) {
            if (t.schema() == null && targetCteName.equals(normalize(t.name().value()))) {
                found = true;
                return defaultResult();
            }
            return super.visitTable(t);
        }
    }
}


