package io.sqm.validate.schema;

import io.sqm.catalog.model.CatalogSchema;
import io.sqm.core.*;
import io.sqm.core.walk.RecursiveNodeVisitor;
import io.sqm.validate.api.StatementValidator;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.api.ValidationResult;
import io.sqm.validate.schema.dialect.SchemaValidationDialect;
import io.sqm.validate.schema.function.FunctionCatalog;
import io.sqm.validate.schema.internal.ModelScope;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.rule.SchemaValidationRuleRegistry;

import java.util.*;

/**
 * Validates {@link Statement} models against {@link CatalogSchema}.
 *
 * <p>Current validation checks:</p>
 * <ul>
 *     <li>table existence and ambiguity for unqualified table names</li>
 *     <li>column existence for qualified and unqualified references</li>
 *     <li>duplicate table aliases in one SELECT scope</li>
 *     <li>basic comparison type compatibility when both operand types are known</li>
 *     <li>MODEL input, rule, reference, and output scopes and final ORDER BY aliases</li>
 * </ul>
 */
public final class SchemaStatementValidator implements StatementValidator {
    private final CatalogSchema schema;
    private final SchemaValidationSettings settings;
    private final SchemaValidationRuleRegistry registry;

    /**
     * Creates validator with default schema-rule registry.
     *
     * @param schema schema model used during validation.
     */
    private SchemaStatementValidator(CatalogSchema schema) {
        this(schema, SchemaValidationSettings.defaults());
    }

    /**
     * Creates validator with explicit rule registry.
     *
     * @param schema          schema model used during validation.
     * @param functionCatalog function signatures catalog.
     */
    private SchemaStatementValidator(CatalogSchema schema, FunctionCatalog functionCatalog) {
        this(schema, SchemaValidationSettings.of(functionCatalog));
    }

    /**
     * Creates validator with explicit validation settings.
     *
     * @param schema   schema model used during validation.
     * @param settings schema validation settings.
     */
    private SchemaStatementValidator(CatalogSchema schema, SchemaValidationSettings settings) {
        this(
            schema,
            settings,
            SchemaValidationRuleRegistry.defaults(
                settings.functionCatalog(),
                settings.limits(),
                settings.additionalRules()
            )
        );
    }

    /**
     * Creates validator with explicit rule registry and settings.
     *
     * @param schema   schema model used during validation.
     * @param settings schema validation settings.
     * @param registry node-rule registry.
     */
    private SchemaStatementValidator(
        CatalogSchema schema,
        SchemaValidationSettings settings,
        SchemaValidationRuleRegistry registry
    ) {
        this.schema = Objects.requireNonNull(schema, "schema");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    /**
     * Creates a validator for the provided schema.
     *
     * @param schema database schema model.
     * @return validator instance.
     */
    public static SchemaStatementValidator of(CatalogSchema schema) {
        return new SchemaStatementValidator(schema);
    }

    /**
     * Creates a validator for the provided schema and function catalog.
     *
     * @param schema          database schema model.
     * @param functionCatalog function signatures catalog.
     * @return validator instance.
     */
    public static SchemaStatementValidator of(CatalogSchema schema, FunctionCatalog functionCatalog) {
        return new SchemaStatementValidator(schema, functionCatalog);
    }

    /**
     * Creates a validator for the provided schema and validation settings.
     *
     * @param schema   database schema model.
     * @param settings schema validation settings.
     * @return validator instance.
     */
    public static SchemaStatementValidator of(CatalogSchema schema, SchemaValidationSettings settings) {
        return new SchemaStatementValidator(schema, settings);
    }

    /**
     * Creates a validator for the provided schema and dialect extension.
     *
     * @param schema  database schema model.
     * @param dialect dialect extension.
     * @return validator instance.
     */
    public static SchemaStatementValidator of(CatalogSchema schema, SchemaValidationDialect dialect) {
        Objects.requireNonNull(dialect, "dialect");
        return new SchemaStatementValidator(schema, dialect.toSettings());
    }

    /**
     * Validates query model against configured schema.
     *
     * @param query query to validate.
     * @return validation result.
     */
    public ValidationResult validate(Query query) {
        return validate((Statement) query);
    }

    /**
     * Validates statement model against configured schema.
     *
     * @param statement statement to validate.
     * @return validation result.
     */
    public ValidationResult validate(Statement statement) {
        Objects.requireNonNull(statement, "statement");
        var visitor = new ValidationVisitor(
            new SchemaValidationContext(
                schema,
                settings.functionCatalog(),
                settings.accessPolicy(),
                settings.tenant(),
                settings.principal()
            ),
            registry
        );
        statement.accept(visitor);
        return new ValidationResult(visitor.problems());
    }

    /**
     * Traversal visitor that manages scope lifecycle and triggers rule dispatch.
     */
    private static final class ValidationVisitor extends RecursiveNodeVisitor<Void> {
        private final SchemaValidationContext context;
        private final SchemaValidationRuleRegistry registry;
        private final Deque<String> modelPaths = new ArrayDeque<>();
        private final Deque<Query> queryBlocks = new ArrayDeque<>();

        /**
         * Creates traversal visitor.
         *
         * @param context  mutable schema validation context.
         * @param registry node-rule registry.
         */
        private ValidationVisitor(
            SchemaValidationContext context,
            SchemaValidationRuleRegistry registry
        ) {
            this.context = context;
            this.registry = registry;
        }

        /**
         * Returns collected validation problems.
         *
         * @return immutable list of problems.
         */
        private List<ValidationProblem> problems() {
            return context.problems();
        }

        @Override
        protected Void defaultResult() {
            return null;
        }

        @Override
        public Void visitWithQuery(WithQuery q) {
            context.pushWithScope();
            queryBlocks.push(q);
            try {
                if (q.recursive()) {
                    for (var cte : q.ctes()) {
                        context.registerCte(cte);
                    }
                }
                registry.validate(q, context);
                for (var cte : q.ctes()) {
                    if (cte.body() != null) {
                        accept(cte.body());
                    }
                    registry.validate(cte, context);
                    if (!q.recursive()) {
                        context.registerCte(cte);
                    }
                }
                accept(q.body());
            } finally {
                queryBlocks.pop();
                context.popWithScope();
            }
            return defaultResult();
        }

        @Override
        public Void visitInsertStatement(InsertStatement statement) {
            context.pushScope();
            try {
                context.registerTableRef(statement.table());
                super.visitInsertStatement(statement);
                registry.validate(statement, context);
                return defaultResult();
            } finally {
                context.popScope();
            }
        }

        @Override
        public Void visitUpdateStatement(UpdateStatement statement) {
            context.pushScope();
            try {
                context.registerTableRef(statement.table());
                for (var from : statement.from()) {
                    context.registerTableRef(from);
                }
                for (var join : statement.joins()) {
                    context.registerTableRef(join.right());
                }
                registerJoinVisibility(List.of(statement.table()), statement.joins());
                super.visitUpdateStatement(statement);
                registry.validate(statement, context);
                return defaultResult();
            } finally {
                context.popScope();
            }
        }

        @Override
        public Void visitDeleteStatement(DeleteStatement statement) {
            context.pushScope();
            try {
                context.registerTableRef(statement.table());
                for (var using : statement.using()) {
                    context.registerTableRef(using);
                }
                for (var join : statement.joins()) {
                    context.registerTableRef(join.right());
                }
                var baseSources = new java.util.ArrayList<TableRef>();
                baseSources.add(statement.table());
                baseSources.addAll(statement.using());
                registerJoinVisibility(baseSources, statement.joins());
                super.visitDeleteStatement(statement);
                registry.validate(statement, context);
                return defaultResult();
            } finally {
                context.popScope();
            }
        }

        @Override
        public Void visitMergeStatement(MergeStatement statement) {
            context.pushScope();
            try {
                context.registerTableRef(statement.target());
                context.registerTableRef(statement.source());
                super.visitMergeStatement(statement);
                registry.validate(statement, context);
                return defaultResult();
            } finally {
                context.popScope();
            }
        }

        @Override
        public Void visitSelectQuery(SelectQuery q) {
            context.pushScope();
            queryBlocks.push(q);
            try {
                // Register visible FROM/JOIN sources before traversal so column validation
                // in SELECT items and predicates can resolve aliases immediately.
                context.registerTableRef(q.from());
                for (var join : q.joins()) {
                    context.registerTableRef(join.right());
                }
                var baseSources = new java.util.ArrayList<TableRef>(1);
                if (q.from() != null) {
                    baseSources.add(q.from());
                }
                registerJoinVisibility(baseSources, q.joins());
                if (q.model() == null) {
                    super.visitSelectQuery(q);
                    registry.validate(q, context);
                }
                else {
                    visitModelSelect(q);
                }
                return defaultResult();
            } finally {
                queryBlocks.pop();
                context.popScope();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Void visitOrderBy(OrderBy order) {
            if (!(queryBlocks.peek() instanceof SelectQuery query) || query.orderBy() != order) {
                return super.visitOrderBy(order);
            }
            for (var item : order.items()) {
                if (item.expr() instanceof ColumnExpr column && column.tableAlias() == null) {
                    var aliases = query.items().stream()
                        .filter(ExprSelectItem.class::isInstance)
                        .map(ExprSelectItem.class::cast)
                        .map(ExprSelectItem::alias)
                        .filter(Objects::nonNull)
                        .filter(alias -> query.model() == null
                            ? alias.value().equalsIgnoreCase(column.name().value())
                            : ModelScope.key(alias).equals(ModelScope.key(column.name()))
                        ).count();
                    if (aliases > 0) {
                        if (aliases > 1) context.addProblem(
                            ValidationProblem.Code.COLUMN_AMBIGUOUS,
                            "Ambiguous ORDER BY projection alias: " + column.name().value(), column, "orderBy.alias");
                        continue;
                    }
                }
                accept(item);
            }
            return defaultResult();
        }

        private void visitModelSelect(SelectQuery query) {
            accept(query.from());
            query.joins().forEach(this::accept);
            accept(query.where());
            accept(query.hierarchical());
            accept(query.groupBy());
            accept(query.having());
            var output = context.modelOutputColumns(query.model().main());
            accept(query.model());
            context.pushDeclaredColumns(output, "model.output");
            try {
                query.items().forEach(this::accept);
                query.hints().forEach(this::accept);
                accept(query.distinct());
                accept(query.topSpec());
                query.windows().forEach(this::accept);
                accept(query.orderBy());
                accept(query.limitOffset());
                accept(query.lockFor());
                registry.validate(query, context);
            } finally {
                context.popDeclaredColumns();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Void visitModelClause(ModelClause clause) {
            var model = context.modelScope(clause);
            for (var index = 0; index < clause.references().size(); index++) {
                modelPaths.push("model.references[" + index + "]");
                try {
                    accept(clause.references().get(index));
                } finally {
                    modelPaths.pop();
                }
            }
            var main = clause.main();
            main.partitions().forEach(this::accept);
            main.dimensions().forEach(this::accept);
            main.measures().forEach(this::accept);
            context.pushDeclaredColumns(model.columns(), "model.main.rules");
            context.pushModelScope(model);
            try {
                registry.validate(clause, context);
                accept(main.rules().iteration());
                for (var index = 0; index < main.rules().rules().size(); index++) {
                    modelPaths.push("model.main.rules.rules[" + index + "]");
                    try {
                        accept(main.rules().rules().get(index));
                    } finally {
                        modelPaths.pop();
                    }
                }
            } finally {
                context.popModelScope();
                context.popDeclaredColumns();
            }
            return defaultResult();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Void visitReferenceModel(ReferenceModel reference) {
            var path = modelPaths.isEmpty() ? "model.reference" : modelPaths.peek();
            context.pushQueryIsolation(path + ".query");
            try {
                accept(reference.query());
            } finally {
                context.popQueryIsolation();
            }
            context.pushQueryOutput(reference.query(), path + ".declarations");
            try {
                reference.dimensions().forEach(this::accept);
                reference.measures().forEach(this::accept);
            } finally {
                context.popDeclaredColumns();
            }
            return defaultResult();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Void visitCellForValues(CellFor.Values values) {
            if (values.values() instanceof QueryExpr query) {
                var path = modelPaths.isEmpty() ? "model.for.values" : modelPaths.peek() + ".target.for.values";
                context.pushQueryIsolation(path);
                try {
                    accept(query);
                } finally {
                    context.popQueryIsolation();
                }
                return defaultResult();
            }
            return super.visitCellForValues(values);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Void visitCellRefExpr(CellRefExpr node) {
            registry.validate(node, context);
            return super.visitCellRefExpr(node);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Void visitModelAggregateExpr(ModelAggregateExpr node) {
            registry.validate(node, context);
            return super.visitModelAggregateExpr(node);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Void visitCurrentDimensionExpr(CurrentDimensionExpr node) {
            registry.validate(node, context);
            return super.visitCurrentDimensionExpr(node);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Void visitIterationNumberExpr(IterationNumberExpr node) {
            registry.validate(node, context);
            return super.visitIterationNumberExpr(node);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Void visitPreviousModelValueExpr(PreviousModelValueExpr node) {
            registry.validate(node, context);
            return super.visitPreviousModelValueExpr(node);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Void visitPresenceValueExpr(PresenceValueExpr node) {
            registry.validate(node, context);
            return super.visitPresenceValueExpr(node);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Void visitCellPresentPredicate(CellPresentPredicate node) {
            registry.validate(node, context);
            return super.visitCellPresentPredicate(node);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Void visitIsAnyPredicate(IsAnyPredicate node) {
            registry.validate(node, context);
            return super.visitIsAnyPredicate(node);
        }

        @Override
        public Void visitColumnExpr(ColumnExpr c) {
            registry.validate(c, context);
            return super.visitColumnExpr(c);
        }

        @Override
        public Void visitAssignment(Assignment assignment) {
            super.visitAssignment(assignment);
            registry.validate(assignment, context);
            return defaultResult();
        }

        /**
         * Validates table-level rules after visiting attached metadata.
         *
         * @param table table reference.
         * @return default result.
         */
        @Override
        public Void visitTable(Table table) {
            super.visitTable(table);
            registry.validate(table, context);
            return defaultResult();
        }

        /**
         * Validates pivot feature support after traversal.
         *
         * @param table pivot table reference.
         * @return default result.
         */
        @Override
        public Void visitPivotTable(PivotTable table) {
            super.visitPivotTable(table);
            registry.validate(table, context);
            return defaultResult();
        }

        /**
         * Validates unpivot feature support after traversal.
         *
         * @param table unpivot table reference.
         * @return default result.
         */
        @Override
        public Void visitUnpivotTable(UnpivotTable table) {
            super.visitUnpivotTable(table);
            registry.validate(table, context);
            return defaultResult();
        }

        /**
         * Validates a pattern-recognition relation in an input-only scope while
         * its inferred output shape remains registered in the enclosing query.
         *
         * @param table pattern-recognition relation.
         * @return default result.
         */
        @Override
        public Void visitPatternRecognitionTable(PatternRecognitionTable table) {
            context.pushScope();
            try {
                context.registerTableRef(table.source());
                accept(table.source());
                accept(table.partitionBy());
                accept(table.orderBy());
                for (var index = 0; index < table.measures().size(); index++) {
                    context.pushPatternExpressionScope(
                        SchemaValidationContext.PatternExpressionKind.MEASURE,
                        "from.matchRecognize.measures[" + index + "]"
                    );
                    try {
                        accept(table.measures().get(index).expression());
                    } finally {
                        context.popPatternExpressionScope();
                    }
                }
                accept(table.rowsPerMatch());
                accept(table.afterMatchSkip());
                accept(table.pattern());
                table.subsets().forEach(this::accept);
                for (var index = 0; index < table.definitions().size(); index++) {
                    context.pushPatternExpressionScope(
                        SchemaValidationContext.PatternExpressionKind.DEFINITION,
                        "from.matchRecognize.definitions[" + index + "]"
                    );
                    try {
                        accept(table.definitions().get(index).condition());
                    } finally {
                        context.popPatternExpressionScope();
                    }
                }
                registry.validate(table, context);
                return defaultResult();
            } finally {
                context.popScope();
            }
        }

        /**
         * Validates JSON table feature support after traversal.
         *
         * @param table JSON table reference
         * @return default result
         */
        @Override
        public Void visitJsonTableRef(JsonTable table) {
            super.visitJsonTableRef(table);
            registry.validate(table, context);
            return defaultResult();
        }

        /**
         * Validates function signature constraints after traversing arguments.
         *
         * @param f function expression.
         * @return default result.
         */
        @Override
        public Void visitFunctionExpr(FunctionExpr f) {
            super.visitFunctionExpr(f);
            registry.validate(f, context);
            return defaultResult();
        }

        @Override
        public Void visitPatternColumnExpr(PatternColumnExpr expression) {
            registry.validate(expression, context);
            return defaultResult();
        }

        @Override
        public Void visitClassifierExpr(ClassifierExpr expression) {
            registry.validate(expression, context);
            return defaultResult();
        }

        @Override
        public Void visitMatchNumberExpr(MatchNumberExpr expression) {
            registry.validate(expression, context);
            return defaultResult();
        }

        @Override
        public Void visitPatternNavigationExpr(PatternNavigationExpr expression) {
            super.visitPatternNavigationExpr(expression);
            registry.validate(expression, context);
            return defaultResult();
        }

        @Override
        public Void visitPatternEvaluationExpr(PatternEvaluationExpr expression) {
            super.visitPatternEvaluationExpr(expression);
            registry.validate(expression, context);
            return defaultResult();
        }

        /**
         * Validates sequence value feature support after normal traversal.
         *
         * @param expr sequence value expression.
         * @return default result.
         */
        @Override
        public Void visitSequenceValueExpr(SequenceValueExpr expr) {
            super.visitSequenceValueExpr(expr);
            registry.validate(expr, context);
            return defaultResult();
        }

        /**
         * Validates hierarchical query feature support after normal traversal.
         *
         * @param clause hierarchical query clause.
         * @return default result.
         */
        @Override
        public Void visitHierarchicalQueryClause(HierarchicalQueryClause clause) {
            super.visitHierarchicalQueryClause(clause);
            registry.validate(clause, context);
            return defaultResult();
        }

        @Override
        public Void visitComparisonPredicate(ComparisonPredicate p) {
            super.visitComparisonPredicate(p);
            registry.validate(p, context);
            return defaultResult();
        }

        /**
         * Validates BETWEEN operand compatibility after normal traversal.
         *
         * @param p BETWEEN predicate.
         * @return default result.
         */
        @Override
        public Void visitBetweenPredicate(BetweenPredicate p) {
            super.visitBetweenPredicate(p);
            registry.validate(p, context);
            return defaultResult();
        }

        /**
         * Validates LIKE-family operand compatibility after normal traversal.
         *
         * @param p LIKE predicate.
         * @return default result.
         */
        @Override
        public Void visitLikePredicate(LikePredicate p) {
            super.visitLikePredicate(p);
            registry.validate(p, context);
            return defaultResult();
        }

        /**
         * Validates IN predicate type compatibility after normal traversal.
         *
         * @param p IN predicate.
         * @return default result.
         */
        @Override
        public Void visitInPredicate(InPredicate p) {
            super.visitInPredicate(p);
            registry.validate(p, context);
            return defaultResult();
        }

        /**
         * Validates USING join semantics after traversal.
         *
         * @param j USING join node.
         * @return default result.
         */
        @Override
        public Void visitUsingJoin(UsingJoin j) {
            super.visitUsingJoin(j);
            registry.validate(j, context);
            return defaultResult();
        }

        /**
         * Validates ON join constraints after traversal.
         *
         * @param j ON join node.
         * @return default result.
         */
        @Override
        public Void visitOnJoin(OnJoin j) {
            super.visitOnJoin(j);
            registry.validate(j, context);
            return defaultResult();
        }

        /**
         * Validates ANY/ALL predicate compatibility after normal traversal.
         *
         * @param p ANY/ALL predicate.
         * @return default result.
         */
        @Override
        public Void visitAnyAllPredicate(AnyAllPredicate p) {
            super.visitAnyAllPredicate(p);
            registry.validate(p, context);
            return defaultResult();
        }

        /**
         * Validates set operation shape and type compatibility after traversal.
         *
         * @param q composite query.
         * @return default result.
         */
        @Override
        public Void visitCompositeQuery(CompositeQuery q) {
            queryBlocks.push(q);
            try {
                super.visitCompositeQuery(q);
                registry.validate(q, context);
                return defaultResult();
            } finally {
                queryBlocks.pop();
            }
        }

        /**
         * Validates IS DISTINCT FROM compatibility after normal traversal.
         *
         * @param p IS DISTINCT FROM predicate.
         * @return default result.
         */
        @Override
        public Void visitIsDistinctFromPredicate(IsDistinctFromPredicate p) {
            super.visitIsDistinctFromPredicate(p);
            registry.validate(p, context);
            return defaultResult();
        }

        /**
         * Validates unary predicate operand type after normal traversal.
         *
         * @param p unary predicate.
         * @return default result.
         */
        @Override
        public Void visitUnaryPredicate(UnaryPredicate p) {
            super.visitUnaryPredicate(p);
            registry.validate(p, context);
            return defaultResult();
        }

        /**
         * Pre-registers aliases visible in each ON join predicate position.
         *
         * @param baseSources a list of table references.
         * @param joins       a list of joins.
         */
        private void registerJoinVisibility(List<? extends TableRef> baseSources, List<? extends Join> joins) {
            var visibleAliases = new LinkedHashSet<String>();
            for (var baseSource : baseSources) {
                context.sourceKey(baseSource).ifPresent(visibleAliases::add);
            }
            for (var join : joins) {
                var rightSource = context.sourceKey(join.right());
                if (join instanceof OnJoin onJoin) {
                    var visibleForOn = new LinkedHashSet<>(visibleAliases);
                    rightSource.ifPresent(visibleForOn::add);
                    context.registerOnJoinVisibleAliases(onJoin, visibleForOn);
                }
                rightSource.ifPresent(visibleAliases::add);
            }
        }
    }
}
