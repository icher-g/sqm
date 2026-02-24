package io.sqm.validate.schema.rule;

import io.sqm.core.*;
import io.sqm.core.walk.RecursiveNodeVisitor;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.catalog.model.CatalogType;
import io.sqm.validate.schema.model.CatalogTypeSemantics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Validates window frame bound expressions and bound ordering.
 */
final class WindowFrameValidationRule implements SchemaValidationRule<SelectQuery> {
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
     * Validates frame specs in WINDOW definitions and inline OVER definitions.
     *
     * @param node select query node.
     * @param context schema validation context.
     */
    @Override
    public void validate(SelectQuery node, SchemaValidationContext context) {
        var windowsByName = indexWindows(node);
        for (var window : node.windows()) {
            if (window.spec() != null) {
                validateFrame(
                    window.spec().frame(),
                    resolveEffectiveOrderBy(window.spec(), windowsByName),
                    window,
                    context,
                    "window.frame"
                );
            }
        }
        node.accept(new FrameVisitor(node, context, windowsByName));
    }

    /**
     * Validates one frame specification.
     *
     * @param frame      frame specification.
     * @param effectiveOrderBy effective order by for the frame owner.
     * @param source source node for diagnostics.
     * @param context validation context.
     * @param clausePath clause path for diagnostics.
     */
    private static void validateFrame(
        FrameSpec frame,
        OrderBy effectiveOrderBy,
        Node source,
        SchemaValidationContext context,
        String clausePath
    ) {
        if (frame == null) {
            return;
        }
        switch (frame) {
            case FrameSpec.Single single -> validateBound(single.bound(), source, context, clausePath);
            case FrameSpec.Between between -> {
                validateBound(between.start(), source, context, clausePath);
                validateBound(between.end(), source, context, clausePath);
                validateBoundOrder(between.start(), between.end(), source, context, clausePath);
            }
            default -> {
            }
        }
        validateRangeUnitRequirements(frame, effectiveOrderBy, source, context, clausePath);
        validateGroupsUnitRequirements(frame, source, context, clausePath);
    }

    /**
     * Indexes windows by normalized name for inheritance resolution.
     *
     * @param select query select query.
     * @return indexed windows.
     */
    private static Map<String, WindowDef> indexWindows(SelectQuery select) {
        var byName = new HashMap<String, WindowDef>(select.windows().size());
        for (var window : select.windows()) {
            if (window.name() != null) {
                byName.putIfAbsent(normalize(window.name().value()), window);
            }
        }
        return Map.copyOf(byName);
    }

    /**
     * Resolves effective ORDER BY for OVER definition with possible base-window inheritance.
     *
     * @param spec OVER definition.
     * @param windowsByName indexed windows by normalized name.
     * @return effective ORDER BY or null.
     */
    private static OrderBy resolveEffectiveOrderBy(OverSpec.Def spec, Map<String, WindowDef> windowsByName) {
        return resolveEffectiveOrderBy(spec, windowsByName, new HashSet<>());
    }

    /**
     * Resolves effective ORDER BY for OVER definition with cycle guard.
     *
     * @param spec OVER definition.
     * @param windowsByName indexed windows by normalized name.
     * @param visiting normalized base-window names currently being visited.
     * @return effective ORDER BY or null.
     */
    private static OrderBy resolveEffectiveOrderBy(
        OverSpec.Def spec,
        Map<String, WindowDef> windowsByName,
        Set<String> visiting
    ) {
        if (spec == null) {
            return null;
        }
        if (spec.orderBy() != null) {
            return spec.orderBy();
        }
        if (spec.baseWindow() == null) {
            return null;
        }
        var baseKey = normalize(spec.baseWindow().value());
        if (!visiting.add(baseKey)) {
            return null;
        }
        try {
            var base = windowsByName.get(baseKey);
            if (base == null || base.spec() == null) {
                return null;
            }
            return resolveEffectiveOrderBy(base.spec(), windowsByName, visiting);
        } finally {
            visiting.remove(baseKey);
        }
    }

    /**
     * Validates RANGE frame constraints that depend on effective ORDER BY.
     *
     * @param frame frame specification.
     * @param effectiveOrderBy effective order by.
     * @param source source node for diagnostics.
     * @param context validation context.
     * @param clausePath clause path for diagnostics.
     */
    private static void validateRangeUnitRequirements(
        FrameSpec frame,
        OrderBy effectiveOrderBy,
        Node source,
        SchemaValidationContext context,
        String clausePath
    ) {
        if (frame.unit() != FrameSpec.Unit.RANGE || !hasOffsetBound(frame)) {
            return;
        }
        if (effectiveOrderBy == null || effectiveOrderBy.items().isEmpty()) {
            context.addProblem(
                ValidationProblem.Code.WINDOW_FRAME_INVALID,
                "RANGE frame with PRECEDING/FOLLOWING offset requires ORDER BY",
                source,
                clausePath
            );
            return;
        }
        if (effectiveOrderBy.items().size() != 1) {
            context.addProblem(
                ValidationProblem.Code.WINDOW_FRAME_INVALID,
                "RANGE frame with PRECEDING/FOLLOWING offset requires single ORDER BY item",
                source,
                clausePath
            );
        }
    }

    /**
     * Validates GROUPS frame offset expression type constraints.
     *
     * @param frame frame specification.
     * @param source source node for diagnostics.
     * @param context validation context.
     * @param clausePath clause path for diagnostics.
     */
    private static void validateGroupsUnitRequirements(
        FrameSpec frame,
        Node source,
        SchemaValidationContext context,
        String clausePath
    ) {
        if (frame.unit() != FrameSpec.Unit.GROUPS) {
            return;
        }
        switch (frame) {
            case FrameSpec.Single single -> validateGroupsBound(single.bound(), source, context, clausePath);
            case FrameSpec.Between between -> {
                validateGroupsBound(between.start(), source, context, clausePath);
                validateGroupsBound(between.end(), source, context, clausePath);
            }
            default -> {
            }
        }
    }

    /**
     * Validates GROUPS PRECEDING/FOLLOWING bound expression as integer-like.
     *
     * @param bound frame bound.
     * @param source source node for diagnostics.
     * @param context validation context.
     * @param clausePath clause path for diagnostics.
     */
    private static void validateGroupsBound(
        BoundSpec bound,
        Node source,
        SchemaValidationContext context,
        String clausePath
    ) {
        Expression expression = switch (bound) {
            case BoundSpec.Preceding preceding -> preceding.expr();
            case BoundSpec.Following following -> following.expr();
            default -> null;
        };
        if (expression == null) {
            return;
        }
        var inferredType = context.inferType(expression);
        if (inferredType.isEmpty() || !CatalogTypeSemantics.isKnown(inferredType.get())) {
            return;
        }
        if (inferredType.get() == CatalogType.INTEGER || inferredType.get() == CatalogType.LONG) {
            return;
        }
        context.addProblem(
            ValidationProblem.Code.WINDOW_FRAME_INVALID,
            "GROUPS frame offset must be INTEGER or LONG but was " + inferredType.get(),
            source,
            clausePath
        );
    }

    /**
     * Returns whether frame contains PRECEDING/FOLLOWING offset bound.
     *
     * @param frame frame specification.
     * @return true when offset bound is present.
     */
    private static boolean hasOffsetBound(FrameSpec frame) {
        return switch (frame) {
            case FrameSpec.Single single -> isOffsetBound(single.bound());
            case FrameSpec.Between between -> isOffsetBound(between.start()) || isOffsetBound(between.end());
            default -> false;
        };
    }

    /**
     * Returns whether bound is PRECEDING/FOLLOWING offset bound.
     *
     * @param bound frame bound.
     * @return true when offset bound.
     */
    private static boolean isOffsetBound(BoundSpec bound) {
        return bound instanceof BoundSpec.Preceding || bound instanceof BoundSpec.Following;
    }

    /**
     * Normalizes identifier for case-insensitive lookup.
     *
     * @param identifier identifier value.
     * @return normalized identifier.
     */
    private static String normalize(String identifier) {
        return identifier.toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * Validates one frame bound expression.
     *
     * @param bound frame bound.
     * @param source source node for diagnostics.
     * @param context validation context.
     * @param clausePath clause path for diagnostics.
     */
    private static void validateBound(
        BoundSpec bound,
        Node source,
        SchemaValidationContext context,
        String clausePath
    ) {
        switch (bound) {
            case BoundSpec.Preceding preceding -> validateBoundExpression(preceding.expr(), "PRECEDING", source, context, clausePath);
            case BoundSpec.Following following -> validateBoundExpression(following.expr(), "FOLLOWING", source, context, clausePath);
            default -> {
            }
        }
    }

    /**
     * Validates one PRECEDING/FOLLOWING bound expression.
     *
     * @param expression bound expression.
     * @param label bound label.
     * @param source source node for diagnostics.
     * @param context validation context.
     * @param clausePath clause path for diagnostics.
     */
    private static void validateBoundExpression(
        Expression expression,
        String label,
        Node source,
        SchemaValidationContext context,
        String clausePath
    ) {
        var inferredType = context.inferType(expression);
        if (inferredType.isPresent() && CatalogTypeSemantics.isKnown(inferredType.get()) && !CatalogTypeSemantics.isNumeric(inferredType.get())) {
            context.addProblem(
                ValidationProblem.Code.WINDOW_FRAME_INVALID,
                label + " bound expression must be numeric but was " + inferredType.get(),
                source,
                clausePath
            );
            return;
        }
        if (expression instanceof LiteralExpr literal && literal.value() instanceof Number number && number.doubleValue() < 0d) {
            context.addProblem(
                ValidationProblem.Code.WINDOW_FRAME_INVALID,
                label + " bound expression must be >= 0 but was " + number,
                source,
                clausePath
            );
        }
    }

    /**
     * Validates relative order of BETWEEN frame bounds.
     *
     * @param start frame start bound.
     * @param end frame end bound.
     * @param source source node for diagnostics.
     * @param context validation context.
     * @param clausePath clause path for diagnostics.
     */
    private static void validateBoundOrder(
        BoundSpec start,
        BoundSpec end,
        Node source,
        SchemaValidationContext context,
        String clausePath
    ) {
        var startRank = boundRank(start);
        var endRank = boundRank(end);
        if (startRank <= endRank) {
            return;
        }
        context.addProblem(
            ValidationProblem.Code.WINDOW_FRAME_INVALID,
            "Window frame start bound must not be after end bound",
            source,
            clausePath
        );
    }

    /**
     * Returns a relative rank for frame bound ordering checks.
     *
     * @param bound frame bound.
     * @return bound rank.
     */
    private static int boundRank(BoundSpec bound) {
        return switch (bound) {
            case BoundSpec.UnboundedPreceding ignored -> 0;
            case BoundSpec.Preceding ignored -> 1;
            case BoundSpec.CurrentRow ignored -> 2;
            case BoundSpec.Following ignored -> 3;
            case BoundSpec.UnboundedFollowing ignored -> 4;
            default -> 2;
        };
    }

    /**
     * Select-local visitor that validates inline OVER(...frame...) expressions.
     */
    private static final class FrameVisitor extends RecursiveNodeVisitor<Void> {
        private final SelectQuery root;
        private final SchemaValidationContext context;
        private final Map<String, WindowDef> windowsByName;

        /**
         * Creates frame visitor.
         *
         * @param root root select query.
         * @param context validation context.
         * @param windowsByName indexed windows by normalized name.
         */
        private FrameVisitor(
            SelectQuery root,
            SchemaValidationContext context,
            Map<String, WindowDef> windowsByName
        ) {
            this.root = root;
            this.context = context;
            this.windowsByName = windowsByName;
        }

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
         * Validates inline OVER definition frame.
         *
         * @param function function expression.
         * @return default result.
         */
        @Override
        public Void visitFunctionExpr(FunctionExpr function) {
            super.visitFunctionExpr(function);
            if (function.over() instanceof OverSpec.Def def) {
                validateFrame(
                    def.frame(),
                    resolveEffectiveOrderBy(def, windowsByName),
                    function,
                    context,
                    "window.frame"
                );
            }
            return defaultResult();
        }
    }
}


