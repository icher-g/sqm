package io.sqm.validate.schema;

import io.sqm.catalog.model.CatalogSchema;
import io.sqm.core.*;
import io.sqm.core.walk.RecursiveNodeVisitor;
import io.sqm.validate.api.StatementValidator;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.api.ValidationResult;
import io.sqm.validate.schema.dialect.SchemaValidationDialect;
import io.sqm.validate.schema.function.FunctionCatalog;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.rule.SchemaValidationRuleRegistry;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * Validates {@link Statement} models against {@link CatalogSchema}.
 *
 * <p>Current validation checks:</p>
 * <ul>
 *     <li>table existence and ambiguity for unqualified table names</li>
 *     <li>column existence for qualified and unqualified references</li>
 *     <li>duplicate table aliases in one SELECT scope</li>
 *     <li>basic comparison type compatibility when both operand types are known</li>
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
            try {
                registry.validate(q, context);
                if (q.recursive()) {
                    for (var cte : q.ctes()) {
                        context.registerCte(cte);
                    }
                }
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
                context.popWithScope();
            }
            return defaultResult();
        }

        @Override
        public Void visitInsertStatement(InsertStatement statement) {
            accept(statement.table());
            accept(statement.source());
            context.pushScope();
            try {
                context.registerTableRef(statement.table());
                statement.conflictUpdateAssignments().forEach(this::accept);
                accept(statement.conflictUpdateWhere());
                accept(statement.result());
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
                accept(statement.table());
                context.registerTableRef(statement.table());
                for (var from : statement.from()) {
                    context.registerTableRef(from);
                }
                for (var join : statement.joins()) {
                    context.registerTableRef(join.right());
                }
                registerJoinVisibility(List.of(statement.table()), statement.joins());
                statement.assignments().forEach(this::accept);
                statement.joins().forEach(this::accept);
                statement.from().forEach(this::accept);
                accept(statement.where());
                accept(statement.result());
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
                accept(statement.table());
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
                statement.using().forEach(this::accept);
                statement.joins().forEach(this::accept);
                accept(statement.where());
                accept(statement.result());
                registry.validate(statement, context);
                return defaultResult();
            } finally {
                context.popScope();
            }
        }

        @Override
        public Void visitSelectQuery(SelectQuery q) {
            context.pushScope();
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
                super.visitSelectQuery(q);
                registry.validate(q, context);
                return defaultResult();
            } finally {
                context.popScope();
            }
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
            super.visitCompositeQuery(q);
            registry.validate(q, context);
            return defaultResult();
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
