package io.sqm.validate.oracle.rule;

import io.sqm.core.*;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.walk.RecursiveNodeVisitor;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.rule.SchemaValidationRule;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Validates Oracle row-pattern recognition structure, scope, and expressions.
 */
public final class OraclePatternRecognitionValidationRule implements SchemaValidationRule<PatternRecognitionTable> {
    private static final Set<String> SUPPORTED_AGGREGATES = Set.of("AVG", "COUNT", "MAX", "MIN", "SUM");
    private final DialectCapabilities capabilities;

    /**
     * Creates an Oracle row-pattern recognition validation rule.
     *
     * @param capabilities Oracle dialect capabilities.
     */
    public OraclePatternRecognitionValidationRule(DialectCapabilities capabilities) {
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
    }

    /** {@inheritDoc} */
    @Override
    public Class<PatternRecognitionTable> nodeType() {
        return PatternRecognitionTable.class;
    }

    /** {@inheritDoc} */
    @Override
    public void validate(PatternRecognitionTable table, SchemaValidationContext context) {
        if (!capabilities.supports(SqlFeature.MATCH_RECOGNIZE)) {
            return;
        }

        var primary = collectPrimaryVariables(table.pattern());
        var definitions = validateDefinitions(table, primary, context);
        var unions = validateSubsets(table, primary, context);
        validateMeasures(table, context);
        validateRowsPerMatch(table, context);
        validateSkip(table, primary, definitions, context);
        validateOutputNames(table, context);

        for (var index = 0; index < table.measures().size(); index++) {
            validateExpression(
                table.measures().get(index).expression(),
                false,
                primary,
                unions,
                "from.matchRecognize.measures[" + index + "]",
                context
            );
        }
        for (var index = 0; index < table.definitions().size(); index++) {
            validateExpression(
                table.definitions().get(index).condition(),
                true,
                primary,
                unions,
                "from.matchRecognize.definitions[" + index + "]",
                context
            );
        }
    }

    private static Map<String, Identifier> collectPrimaryVariables(MatchPattern pattern) {
        var variables = new LinkedHashMap<String, Identifier>();
        pattern.accept(new RecursiveNodeVisitor<Void>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitPatternVariable(MatchPattern.Variable variable) {
                variables.putIfAbsent(key(variable.name()), variable.name());
                return defaultResult();
            }
        });
        return variables;
    }

    private static Set<String> validateDefinitions(
        PatternRecognitionTable table,
        Map<String, Identifier> primary,
        SchemaValidationContext context
    ) {
        var definitions = new LinkedHashSet<String>();
        for (var index = 0; index < table.definitions().size(); index++) {
            var definition = table.definitions().get(index);
            var name = key(definition.variable());
            var path = "from.matchRecognize.definitions[" + index + "]";
            if (!definitions.add(name)) {
                invalid(context, definition, "Duplicate pattern definition: " + definition.variable().value(), path);
            }
            if (!primary.containsKey(name)) {
                invalid(context, definition, "Definition does not name a primary pattern variable: "
                    + definition.variable().value(), path);
            }
        }
        for (var entry : primary.entrySet()) {
            if (!definitions.contains(entry.getKey())) {
                invalid(context, table, "Primary pattern variable requires an explicit DEFINE item: "
                    + entry.getValue().value(), "from.matchRecognize.definitions");
            }
        }
        return definitions;
    }

    private static Set<String> validateSubsets(
        PatternRecognitionTable table,
        Map<String, Identifier> primary,
        SchemaValidationContext context
    ) {
        var unions = new LinkedHashSet<String>();
        for (var index = 0; index < table.subsets().size(); index++) {
            var subset = table.subsets().get(index);
            var subsetName = key(subset.name());
            var path = "from.matchRecognize.subsets[" + index + "]";
            if (primary.containsKey(subsetName)) {
                invalid(context, subset, "Subset name collides with a primary pattern variable: "
                    + subset.name().value(), path);
            }
            if (!unions.add(subsetName)) {
                invalid(context, subset, "Duplicate subset name: " + subset.name().value(), path);
            }
            var members = new LinkedHashSet<String>();
            for (var member : subset.variables()) {
                var memberName = key(member);
                if (!members.add(memberName)) {
                    invalid(context, subset, "Duplicate subset member: " + member.value(), path);
                }
                if (!primary.containsKey(memberName)) {
                    invalid(context, subset, "Subset member is not a primary pattern variable: "
                        + member.value(), path);
                }
            }
        }
        return unions;
    }

    private static void validateMeasures(PatternRecognitionTable table, SchemaValidationContext context) {
        var aliases = new LinkedHashSet<String>();
        for (var index = 0; index < table.measures().size(); index++) {
            var measure = table.measures().get(index);
            if (!aliases.add(key(measure.alias()))) {
                invalid(context, measure, "Duplicate measure alias: " + measure.alias().value(),
                    "from.matchRecognize.measures[" + index + "]");
            }
        }
    }

    private static void validateRowsPerMatch(PatternRecognitionTable table, SchemaValidationContext context) {
        if (table.rowsPerMatch().emptyMatchHandling() != RowsPerMatch.EmptyMatchHandling.DEFAULT) {
            invalid(context, table.rowsPerMatch(),
                "Oracle MATCH_RECOGNIZE does not support explicit empty or unmatched-row handling",
                "from.matchRecognize.rowsPerMatch");
        }
    }

    private static void validateSkip(
        PatternRecognitionTable table,
        Map<String, Identifier> primary,
        Set<String> definitions,
        SchemaValidationContext context
    ) {
        var skip = table.afterMatchSkip();
        if (skip.kind() != AfterMatchSkip.Kind.TO_VARIABLE) {
            return;
        }
        var target = key(skip.variable());
        if (!primary.containsKey(target) || !definitions.contains(target)) {
            invalid(context, skip, "Oracle skip target must be a defined primary pattern variable: "
                + skip.variable().value(), "from.matchRecognize.afterMatchSkip");
        }
    }

    private static void validateOutputNames(PatternRecognitionTable table, SchemaValidationContext context) {
        if (table.rowsPerMatch().mode() != RowsPerMatch.Mode.ONE) {
            return;
        }
        var names = new LinkedHashSet<String>();
        if (table.partitionBy() != null) {
            for (var index = 0; index < table.partitionBy().items().size(); index++) {
                var expression = table.partitionBy().items().get(index);
                if (expression instanceof ColumnExpr column && !names.add(key(column.name()))) {
                    invalid(context, expression, "Duplicate inferred output column: " + column.name().value(),
                        "from.matchRecognize.partitionBy[" + index + "]");
                }
            }
        }
        for (var index = 0; index < table.measures().size(); index++) {
            var measure = table.measures().get(index);
            if (!names.add(key(measure.alias()))) {
                invalid(context, measure, "Duplicate inferred output column: " + measure.alias().value(),
                    "from.matchRecognize.measures[" + index + "]");
            }
        }
    }

    private static void validateExpression(
        Expression expression,
        boolean definition,
        Map<String, Identifier> primary,
        Set<String> unions,
        String path,
        SchemaValidationContext context
    ) {
        expression.accept(new RecursiveNodeVisitor<Void>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitPatternColumnExpr(PatternColumnExpr node) {
                validateVariable(node.variable(), node);
                return defaultResult();
            }

            @Override
            public Void visitClassifierExpr(ClassifierExpr node) {
                if (node.variable() != null) {
                    invalid(context, node, "Oracle CLASSIFIER does not accept a pattern-variable argument", path);
                }
                return defaultResult();
            }

            @Override
            public Void visitPatternEvaluationExpr(PatternEvaluationExpr node) {
                if (definition && node.mode() == PatternEvaluationExpr.Mode.FINAL) {
                    invalid(context, node, "Oracle FINAL evaluation is not allowed in DEFINE", path);
                }
                if (!isSupportedEvaluationTarget(node.expression(), context)) {
                    invalid(context, node, "RUNNING or FINAL must wrap a supported navigation or aggregate expression", path);
                }
                if (node.expression() instanceof PatternNavigationExpr navigation
                    && (navigation.kind() == PatternNavigationExpr.Kind.PREV
                    || navigation.kind() == PatternNavigationExpr.Kind.NEXT)) {
                    invalid(context, node, "Oracle PREV and NEXT cannot use RUNNING or FINAL evaluation", path);
                }
                return super.visitPatternEvaluationExpr(node);
            }

            @Override
            public Void visitPatternNavigationExpr(PatternNavigationExpr node) {
                if ((node.kind() == PatternNavigationExpr.Kind.PREV || node.kind() == PatternNavigationExpr.Kind.NEXT)
                    && containsEvaluation(node.expression())) {
                    invalid(context, node, "Oracle PREV and NEXT cannot contain RUNNING or FINAL evaluation", path);
                }
                validateOffset(node.offset(), node, path, context);
                return super.visitPatternNavigationExpr(node);
            }

            @Override
            public Void visitFunctionExpr(FunctionExpr node) {
                if (node.over() != null) {
                    invalid(context, node, "Window expressions are not allowed in Oracle MATCH_RECOGNIZE expressions", path);
                }
                var signature = context.functionCatalog().resolve(functionName(node)).orElse(null);
                if (signature != null && signature.aggregate()) {
                    var name = functionName(node).toUpperCase(Locale.ROOT);
                    if (!SUPPORTED_AGGREGATES.contains(name)) {
                        invalid(context, node, "Unsupported Oracle row-pattern aggregate: " + name, path);
                    }
                    if (Boolean.TRUE.equals(node.distinctArg())) {
                        invalid(context, node, "DISTINCT is not allowed in Oracle row-pattern aggregates", path);
                    }
                    if (node.orderBy() != null || node.withinGroup() != null || node.filter() != null) {
                        invalid(context, node, "Ordered, WITHIN GROUP, and FILTER aggregate forms are not allowed in Oracle MATCH_RECOGNIZE", path);
                    }
                }
                return super.visitFunctionExpr(node);
            }

            private void validateVariable(Identifier variable, Node node) {
                var variableName = key(variable);
                if (!primary.containsKey(variableName) && !unions.contains(variableName)) {
                    invalid(context, node, "Unknown pattern variable: " + variable.value(), path);
                }
            }
        });
    }

    private static boolean isSupportedEvaluationTarget(Expression expression, SchemaValidationContext context) {
        if (expression instanceof PatternNavigationExpr) {
            return true;
        }
        if (!(expression instanceof FunctionExpr function)) {
            return false;
        }
        return context.functionCatalog().resolve(functionName(function))
            .map(signature -> signature.aggregate()
                && SUPPORTED_AGGREGATES.contains(functionName(function).toUpperCase(Locale.ROOT)))
            .orElse(false);
    }

    private static boolean containsEvaluation(Expression expression) {
        var found = new boolean[1];
        expression.accept(new RecursiveNodeVisitor<Void>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitPatternEvaluationExpr(PatternEvaluationExpr node) {
                found[0] = true;
                return defaultResult();
            }
        });
        return found[0];
    }

    private static void validateOffset(
        Expression offset,
        PatternNavigationExpr navigation,
        String path,
        SchemaValidationContext context
    ) {
        if (offset == null) {
            return;
        }
        if (offset instanceof NegativeArithmeticExpr) {
            invalid(context, navigation, "Pattern navigation offset must be non-negative", path);
            return;
        }
        if (!(offset instanceof LiteralExpr literal)) {
            return;
        }
        if (!(literal.value() instanceof Number number)
            || number.longValue() < 0
            || number.doubleValue() != Math.rint(number.doubleValue())) {
            invalid(context, navigation, "Pattern navigation offset must be a non-negative integer", path);
        }
    }

    private static String functionName(FunctionExpr function) {
        return function.name().parts().getLast().value();
    }

    private static String key(Identifier identifier) {
        return identifier.quoted() ? identifier.value() : identifier.value().toUpperCase(Locale.ROOT);
    }

    private static void invalid(SchemaValidationContext context, Node node, String message, String path) {
        context.addProblem(ValidationProblem.Code.DIALECT_CLAUSE_INVALID, message, node, path);
    }
}
