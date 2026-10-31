package io.sqm.validate.oracle.rule;

import io.sqm.core.*;
import io.sqm.core.walk.RecursiveNodeVisitor;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.ModelScope;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.model.CatalogTypeSemantics;
import io.sqm.validate.schema.rule.SchemaValidationRule;

import java.util.HashSet;
import java.util.List;

/**
 * Resolves Oracle MODEL declarations, writable measures, cell coordinates, and
 * expression placement against the closed main/reference model namespaces.
 * Input-row and query correlation boundaries are managed by schema traversal.
 */
public final class ModelSemanticValidationRule implements SchemaValidationRule<ModelClause> {
    /**
     * Creates the MODEL semantic rule.
     */
    public ModelSemanticValidationRule() {
    }

    private static void declarations(List<ModelColumn> partitions, List<ModelColumn> dimensions,
        List<ModelColumn> measures, String path, SchemaValidationContext context) {
        var names = new HashSet<String>();
        var groups = List.of(partitions, dimensions, measures);
        var labels = List.of("partitions", "dimensions", "measures");
        for (var group = 0; group < groups.size(); group++) {
            for (var index = 0; index < groups.get(group).size(); index++) {
                var column = groups.get(group).get(index);
                if (!names.add(ModelScope.key(column.name()))) {
                    invalid(context, column, "duplicate declared column name", path + "." + labels.get(group) + "[" + index + "].name");
                }
            }
        }
    }

    private static boolean contains(List<ModelColumn> columns, Identifier name) {
        return columns.stream().anyMatch(column -> ModelScope.key(column.name()).equals(ModelScope.key(name)));
    }

    private static void invalid(SchemaValidationContext context, Node node, String message, String path) {
        context.addProblem(ValidationProblem.Code.DIALECT_CLAUSE_INVALID, "MODEL " + message, node, path);
    }

    private static boolean single(CellSelector selector) {
        return selector instanceof CellSelector.Value || selector instanceof CellSelector.Condition(Predicate predicate)
            && predicate instanceof ComparisonPredicate comparison && comparison.operator() == ComparisonOperator.EQ;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<ModelClause> nodeType() {
        return ModelClause.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate(ModelClause clause, SchemaValidationContext context) {
        var main = clause.main();
        declarations(main.partitions(), main.dimensions(), main.measures(), "model.main", context);
        var names = new HashSet<String>();
        if (main.name() != null) names.add(ModelScope.key(main.name()));
        for (var index = 0; index < clause.references().size(); index++) {
            var reference = clause.references().get(index);
            var path = "model.references[" + index + "]";
            if (!names.add(ModelScope.key(reference.name()))) invalid(context, reference, "duplicate model name", path + ".name");
            declarations(List.of(), reference.dimensions(), reference.measures(), path, context);
        }
        var rules = main.rules();
        if (rules.iteration() != null && rules.iteration().until() != null) {
            rules.iteration().until().accept(new Expressions(clause, context, Phase.UNTIL, null,
                "model.main.rules.iteration.until"));
        }
        for (var index = 0; index < rules.rules().size(); index++) {
            var rule = rules.rules().get(index);
            var path = "model.main.rules.rules[" + index + "]";
            if (!contains(main.measures(), rule.target().measure())) {
                invalid(context, rule.target(), "assignment target must name a declared main-model measure", path + ".target.measure");
            }
            var target = new Expressions(clause, context, Phase.TARGET, rule, path + ".target");
            target.cellDepth = 1;
            target.addresses(rule.target().addresses(), main.dimensions(), false, null);
            if (rule.orderBy() != null) {
                for (var item : rule.orderBy().items()) {
                    if (item.ordinal() != null) invalid(context, item, "rule ORDER BY cannot use positional ordinals", path + ".orderBy");
                }
                rule.orderBy().accept(new Expressions(clause, context, Phase.ORDER, rule, path + ".orderBy"));
            }
            rule.value().accept(new Expressions(clause, context, Phase.RHS, rule, path + ".value"));
            var model = context.modelScope();
            var measure = model == null ? null : model.resolve(null, rule.target().measure());
            var valueType = context.inferType(rule.value());
            if (measure != null && valueType.isPresent() && !CatalogTypeSemantics.comparable(measure.type(), valueType.get())) {
                context.addProblem(ValidationProblem.Code.TYPE_MISMATCH, "MODEL assignment type " + valueType.get() + " is incompatible with measure type " + measure.type(), rule, path + ".value");
            }
        }
    }

    private enum Phase {TARGET, RHS, UNTIL, ORDER}

    /**
     * A rule-local traversal; cell/selector depth must be shared across nested expressions.
     */
    private static final class Expressions extends RecursiveNodeVisitor<Void> {
        private final ModelClause clause;
        private final SchemaValidationContext context;
        private final Phase phase;
        private final ModelRule rule;
        private final String path;
        private int cellDepth;
        private int selectorDepth;
        private int conditionDepth;

        private Expressions(ModelClause clause, SchemaValidationContext context, Phase phase, ModelRule rule, String path) {
            this.clause = clause;
            this.context = context;
            this.phase = phase;
            this.rule = rule;
            this.path = path;
        }

        @Override
        protected Void defaultResult() {
            return null;
        }

        private void fail(Node node, String message) {
            invalid(context, node, message, path);
        }

        private void addresses(List<? extends CellAddress> addresses, List<ModelColumn> dimensions, boolean read, Identifier qualifier) {
            var position = 0;
            for (var address : addresses) {
                if (address instanceof CellFor generator) {
                    for (var dimension : generator.dimensions()) {
                        if (position >= dimensions.size() || !ModelScope.key(dimensions.get(position).name()).equals(ModelScope.key(dimension))) {
                            fail(generator, "FOR dimensions must match the declared dimension positions");
                        }
                        position++;
                    }
                    accept(generator);
                    continue;
                }
                var selector = (CellSelector) address;
                if (position < dimensions.size()) {
                    var dimension = dimensions.get(position);
                    if (selector instanceof CellSelector.Condition(Predicate predicate)) {
                        predicate.accept(new RecursiveNodeVisitor<Void>() {
                            @Override
                            protected Void defaultResult() {
                                return null;
                            }

                            @Override
                            public Void visitColumnExpr(ColumnExpr column) {
                                if (column.tableAlias() != null || !ModelScope.key(column.name()).equals(ModelScope.key(dimension.name()))) {
                                    fail(column, "symbolic selector must reference its own dimension only");
                                }
                                return null;
                            }

                            @Override
                            public Void visitCurrentDimensionExpr(CurrentDimensionExpr expression) {
                                return null;
                            }

                            @Override
                            public Void visitCellRefExpr(CellRefExpr expression) {
                                return null;
                            }
                        });
                    }
                    else
                        if (selector instanceof CellSelector.Value value) {
                            var model = context.modelScope();
                            var metadata = model == null ? null : model.resolve(qualifier, dimension.name());
                            if (metadata != null) {
                                var type = context.inferType(value.value());
                                if (type.isPresent() && !CatalogTypeSemantics.comparable(metadata.type(), type.get())) {
                                    context.addProblem(ValidationProblem.Code.TYPE_MISMATCH, "MODEL coordinate type does not match dimension",
                                        value, path);
                                }
                            }
                        }
                }
                if (read && !single(selector)) fail(selector, "cell reads must resolve to a single cell; use a MODEL aggregate for multiple cells");
                selectorDepth++;
                if (selector instanceof CellSelector.Condition) conditionDepth++;
                accept(selector);
                if (selector instanceof CellSelector.Condition) conditionDepth--;
                selectorDepth--;
                position++;
            }
            if (position != dimensions.size()) fail(clause, "cell address must qualify every declared dimension exactly once");
        }

        @Override
        public Void visitQueryExpr(QueryExpr query) {
            fail(query, "subqueries are allowed only in reference queries and FOR value sources");
            return null;
        }

        @Override
        public Void visitCellForValues(CellFor.Values values) {
            if (values.values() instanceof QueryExpr query) {
                query.subquery().accept(new RecursiveNodeVisitor<Void>() {
                    @Override
                    protected Void defaultResult() {
                        return null;
                    }

                    @Override
                    protected Void accept(Node node) {
                        if (node instanceof ParamExpr || node instanceof QueryExpr || node instanceof QueryTable || node instanceof WithQuery) {
                            fail(node, "FOR source queries cannot contain binds, subqueries, or WITH queries");
                        }
                        return super.accept(node);
                    }

                    @Override
                    public Void visitTable(Table table) {
                        if (context.isCteReference(table)) fail(table, "FOR source queries cannot reference WITH tables");
                        return super.visitTable(table);
                    }

                    @Override
                    public Void visitWithQuery(WithQuery with) {
                        fail(with, "FOR source queries cannot contain WITH queries");
                        return super.visitWithQuery(with);
                    }
                });
                return null;
            }
            return super.visitCellForValues(values);
        }

        @Override
        public Void visitCellRefExpr(CellRefExpr cell) {
            if (phase == Phase.ORDER) fail(cell, "rule ORDER BY must resolve to dimension or measure columns");
            if (cellDepth > 1) fail(cell, "cell references allow only one level of nesting");
            if (phase == Phase.TARGET && clause.main().rules().order() == RuleOrder.AUTOMATIC
                && (cell.model() == null || clause.main().name() != null && ModelScope.key(cell.model()).equals(ModelScope.key(clause.main().name())))
                && clause.main().rules().rules().stream().anyMatch(r ->
                ModelScope.key(r.target().measure()).equals(ModelScope.key(cell.measure())))) {
                fail(cell, "AUTOMATIC ORDER target coordinates may read only static measures");
            }
            var reference = cell.model() == null || clause.main().name() != null
                && ModelScope.key(clause.main().name()).equals(ModelScope.key(cell.model())) ? null
                : clause.references().stream().filter(r -> ModelScope.key(r.name()).equals(ModelScope.key(cell.model()))).findFirst().orElse(null);
            if (cell.model() != null && reference == null && (clause.main().name() == null
                || !ModelScope.key(clause.main().name()).equals(ModelScope.key(cell.model())))) {
                fail(cell, "unknown model qualifier " + cell.model().value());
            }
            var measures = reference == null ? clause.main().measures() : reference.measures();
            if (!contains(measures, cell.measure())) fail(cell, "cell reference must name a declared measure");
            cellDepth++;
            addresses(cell.selectors(), reference == null ? clause.main().dimensions() : reference.dimensions(), true, cell.model());
            cellDepth--;
            return null;
        }

        @Override
        public Void visitModelAggregateExpr(ModelAggregateExpr aggregate) {
            if (phase != Phase.RHS) fail(aggregate, "aggregate cell expressions are allowed only on rule right-hand sides");
            if (cellDepth > 0) fail(aggregate, "nested cell references must be single-cell references");
            var function = aggregate.aggregate();
            var signature = context.functionCatalog().resolve(String.join(".", function.name().values()));
            if (signature.isPresent() && !signature.get().aggregate() || function.over() != null) fail(aggregate, "cell aggregate requires an aggregate function");
            if (function.args() == null || function.args().size() != 1
                || !(function.args().getFirst() instanceof FunctionExpr.Arg.ExprArg argument)
                || !(argument.expr() instanceof ColumnExpr column) || column.tableAlias() != null
                || !contains(clause.main().measures(), column.name())) {
                fail(aggregate, "aggregate argument must be a declared main-model measure");
            }
            cellDepth++;
            addresses(aggregate.selectors(), clause.main().dimensions(), false, null);
            cellDepth--;
            return null;
        }

        @Override
        public Void visitCurrentDimensionExpr(CurrentDimensionExpr current) {
            if (phase != Phase.RHS || current.dimension() == null && selectorDepth == 0) fail(current,
                "CV is allowed only on rule right-hand sides; bare CV() requires a cell coordinate");
            if (current.dimension() != null && !contains(clause.main().dimensions(), current.dimension())
                && !contains(clause.main().partitions(), current.dimension())) fail(current, "CV must name a declared dimension or partition");
            return null;
        }

        @Override
        public Void visitIterationNumberExpr(IterationNumberExpr number) {
            if (clause.main().rules().iteration() == null || phase == Phase.ORDER) fail(number, "ITERATION_NUMBER requires iterative rules");
            return null;
        }

        @Override
        public Void visitPreviousModelValueExpr(PreviousModelValueExpr previous) {
            if (phase != Phase.UNTIL) fail(previous, "PREVIOUS is allowed only in ITERATE UNTIL");
            return super.visitPreviousModelValueExpr(previous);
        }

        @Override
        public Void visitPresenceValueExpr(PresenceValueExpr presence) {
            if (phase != Phase.RHS) fail(presence, "presence functions are allowed only on rule right-hand sides");
            return super.visitPresenceValueExpr(presence);
        }

        @Override
        public Void visitCellPresentPredicate(CellPresentPredicate present) {
            if (phase != Phase.RHS && phase != Phase.UNTIL) fail(present, "IS PRESENT requires a rule value or UNTIL condition");
            return super.visitCellPresentPredicate(present);
        }

        @Override
        public Void visitIsAnyPredicate(IsAnyPredicate any) {
            if (selectorDepth == 0 || conditionDepth == 0) fail(any, "ANY requires a symbolic cell selector");
            return super.visitIsAnyPredicate(any);
        }

        @Override
        public Void visitColumnExpr(ColumnExpr column) {
            if (phase == Phase.ORDER && !contains(clause.main().dimensions(), column.name())
                && !contains(clause.main().measures(), column.name())) fail(column, "rule ORDER BY must name a dimension or measure");
            return null;
        }

        @Override
        public Void visitFunctionExpr(FunctionExpr function) {
            var signature = context.functionCatalog().resolve(String.join(".", function.name().values()));
            if (conditionDepth > 0 && phase == Phase.TARGET && signature.isPresent() && signature.get().aggregate()) {
                fail(function, "target symbolic conditions cannot contain aggregates");
            }
            if (function.over() != null) {
                var mode = rule == null ? null : rule.mode() == null ? clause.main().rules().defaultMode() : rule.mode();
                var allSymbolic = rule != null && rule.target().addresses().stream().allMatch(CellSelector.Condition.class::isInstance);
                var symbolic = rule != null && rule.target().addresses().stream().anyMatch(CellSelector.Condition.class::isInstance);
                boolean update = mode == RuleMode.UPDATE || mode != RuleMode.UPSERT_ALL && symbolic || mode == RuleMode.UPSERT_ALL && allSymbolic;
                if (phase != Phase.RHS || !update || rule.orderBy() != null || rule.target().addresses().stream().anyMatch(CellFor.class::isInstance)) {
                    fail(function, "analytic functions require UPDATE rules without FOR targets or rule ORDER BY");
                }
                if (function.args() != null) for (var arg : function.args())
                    arg.accept(new RecursiveNodeVisitor<Void>() {
                        @Override
                        protected Void defaultResult() {
                            return null;
                        }

                        @Override
                        public Void visitCellRefExpr(CellRefExpr cell) {
                            fail(cell, "analytic function arguments before OVER cannot contain cell references");
                            return null;
                        }
                    });
                function.over().accept(new RecursiveNodeVisitor<Void>() {
                    @Override
                    protected Void defaultResult() {
                        return null;
                    }

                    @Override
                    public Void visitFunctionExpr(FunctionExpr nested) {
                        if (context.functionCatalog().resolve(String.join(".", nested.name().values())).map(s -> s.aggregate()).orElse(false)) {
                            fail(nested, "analytic OVER arguments cannot contain aggregates");
                        }
                        return super.visitFunctionExpr(nested);
                    }
                });
            }
            return super.visitFunctionExpr(function);
        }
    }
}
