package io.sqm.validate.schema.rule;

import io.sqm.core.Node;
import io.sqm.validate.schema.SchemaValidationLimits;
import io.sqm.validate.schema.function.DefaultFunctionCatalog;
import io.sqm.validate.schema.function.FunctionCatalog;
import io.sqm.validate.schema.internal.SchemaValidationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Registry that dispatches node validation to registered schema rules.
 */
public final class SchemaValidationRuleRegistry {
    private final List<SchemaValidationRule<? extends Node>> rules;

    /**
     * Creates a registry with immutable rule list.
     *
     * @param rules validation rules.
     */
    private SchemaValidationRuleRegistry(List<SchemaValidationRule<? extends Node>> rules) {
        this.rules = List.copyOf(rules);
    }

    /**
     * Creates default schema validation rule set.
     *
     * @return default rule registry.
     */
    public static SchemaValidationRuleRegistry defaults() {
        return defaults(DefaultFunctionCatalog.standard(), SchemaValidationLimits.unlimited(), List.of());
    }

    /**
     * Creates default schema validation rule set with explicit function catalog.
     *
     * @param functionCatalog function catalog used by function-signature rule.
     * @return default rule registry.
     */
    public static SchemaValidationRuleRegistry defaults(FunctionCatalog functionCatalog) {
        return defaults(functionCatalog, SchemaValidationLimits.unlimited(), List.of());
    }

    /**
     * Creates default schema validation rule set with additional rules appended.
     *
     * @param functionCatalog function catalog used by function-signature rule.
     * @param additionalRules additional rules appended after default rules.
     * @return default rule registry.
     */
    public static SchemaValidationRuleRegistry defaults(
        FunctionCatalog functionCatalog,
        SchemaValidationLimits limits,
        List<SchemaValidationRule<? extends Node>> additionalRules
    ) {
        var projectionShapeInspector = new DefaultProjectionShapeInspector();
        var scalarSubqueryShapeValidator = new DefaultScalarSubqueryShapeValidator(projectionShapeInspector);
        var rules = new ArrayList<>(List.of(
            new StructuralLimitsValidationRule(limits),
            new ColumnReferenceValidationRule(),
            new ColumnAccessValidationRule(),
            new ComparisonTypeValidationRule(scalarSubqueryShapeValidator),
            new BetweenTypeValidationRule(scalarSubqueryShapeValidator),
            new LikeTypeValidationRule(scalarSubqueryShapeValidator),
            new InPredicateTypeValidationRule(projectionShapeInspector),
            new AnyAllPredicateTypeValidationRule(projectionShapeInspector),
            new IsDistinctFromTypeValidationRule(scalarSubqueryShapeValidator),
            new UnaryPredicateTypeValidationRule(scalarSubqueryShapeValidator),
            new WithQueryValidationRule(projectionShapeInspector),
            new CteDefinitionValidationRule(projectionShapeInspector),
            new OrderByOrdinalValidationRule(),
            new CompositeOrderByOrdinalValidationRule(projectionShapeInspector),
            new LimitOffsetValidationRule(scalarSubqueryShapeValidator),
            new CompositeLimitOffsetValidationRule(scalarSubqueryShapeValidator),
            new GroupByOrdinalValidationRule(),
            new WindowDefinitionValidationRule(),
            new WindowInheritanceValidationRule(),
            new WindowReferenceValidationRule(),
            new WindowFrameValidationRule(),
            new LockingClauseValidationRule(),
            new SetOperationValidationRule(projectionShapeInspector),
            new OnJoinValidationRule(),
            new UsingJoinValidationRule(),
            new SelectAggregationValidationRule(functionCatalog),
            new FunctionAllowlistValidationRule(),
            new FunctionSignatureValidationRule(functionCatalog)
        ));
        rules.addAll(additionalRules);
        return new SchemaValidationRuleRegistry(rules);
    }

    /**
     * Runs all matching rules for the provided node.
     *
     * @param node visited node.
     * @param context validation context.
     */
    public void validate(Node node, SchemaValidationContext context) {
        for (var rule : rules) {
            applyRule(rule, node, context);
        }
    }

    /**
     * Applies a typed rule when current node matches rule type.
     *
     * @param rule typed rule.
     * @param node node being validated.
     * @param context validation context.
     * @param <N> rule node type.
     */
    private static <N extends Node> void applyRule(
        SchemaValidationRule<N> rule,
        Node node,
        SchemaValidationContext context
    ) {
        if (!rule.nodeType().isInstance(node)) {
            return;
        }
        rule.validate(rule.nodeType().cast(node), context);
    }
}
