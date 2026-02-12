package io.sqm.validate.postgresql.rule;

import io.sqm.core.*;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.walk.RecursiveNodeVisitor;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.rule.SchemaValidationRule;

import java.util.Objects;

/**
 * Validates PostgreSQL SELECT-level and expression-level features against
 * versioned dialect capabilities.
 */
public final class PostgresSelectFeatureValidationRule implements SchemaValidationRule<SelectQuery> {
    private final DialectCapabilities capabilities;
    private final SqlDialectVersion version;

    /**
     * Creates a PostgreSQL SELECT feature validation rule.
     *
     * @param capabilities dialect capabilities.
     * @param version PostgreSQL version.
     */
    public PostgresSelectFeatureValidationRule(DialectCapabilities capabilities, SqlDialectVersion version) {
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
        this.version = Objects.requireNonNull(version, "version");
    }

    @Override
    public Class<SelectQuery> nodeType() {
        return SelectQuery.class;
    }

    @Override
    public void validate(SelectQuery node, SchemaValidationContext context) {
        if (node.distinct() != null && !node.distinct().items().isEmpty()) {
            require(context, node, SqlFeature.DISTINCT_ON, "select.distinct");
        }

        if (node.lockFor() != null) {
            validateLocking(node.lockFor(), context);
        }

        new SelectFeatureWalker(context).acceptNode(node);
    }

    private void validateLocking(LockingClause lockingClause, SchemaValidationContext context) {
        require(context, lockingClause, SqlFeature.LOCKING_CLAUSE, "select.lock");
        switch (lockingClause.mode()) {
            case UPDATE -> {
            }
            case SHARE -> require(context, lockingClause, SqlFeature.LOCKING_SHARE, "select.lock");
            case KEY_SHARE -> require(context, lockingClause, SqlFeature.LOCKING_KEY_SHARE, "select.lock");
            case NO_KEY_UPDATE -> require(context, lockingClause, SqlFeature.LOCKING_NO_KEY_UPDATE, "select.lock");
        }

        if (!lockingClause.ofTables().isEmpty()) {
            require(context, lockingClause, SqlFeature.LOCKING_OF, "select.lock");
        }
        if (lockingClause.nowait()) {
            require(context, lockingClause, SqlFeature.LOCKING_NOWAIT, "select.lock");
        }
        if (lockingClause.skipLocked()) {
            require(context, lockingClause, SqlFeature.LOCKING_SKIP_LOCKED, "select.lock");
        }
    }

    private void require(
        SchemaValidationContext context,
        Node node,
        SqlFeature feature,
        String clausePath
    ) {
        if (capabilities.supports(feature)) {
            return;
        }
        context.addProblem(
            ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
            "PostgreSQL " + version + " does not support " + feature.description(),
            node,
            clausePath
        );
    }

    private final class SelectFeatureWalker extends RecursiveNodeVisitor<Void> {
        private final SchemaValidationContext context;

        private SelectFeatureWalker(SchemaValidationContext context) {
            this.context = context;
        }

        private void acceptNode(Node node) {
            if (node != null) {
                node.accept(this);
            }
        }

        @Override
        protected Void defaultResult() {
            return null;
        }

        @Override
        public Void visitOrderItem(OrderItem i) {
            if (i.usingOperator() != null) {
                require(context, i, SqlFeature.ORDER_BY_USING, "select.order");
            }
            return super.visitOrderItem(i);
        }

        @Override
        public Void visitLikePredicate(LikePredicate p) {
            if (p.mode() == LikeMode.ILIKE) {
                require(context, p, SqlFeature.ILIKE_PREDICATE, "predicate.like");
            } else if (p.mode() == LikeMode.SIMILAR_TO) {
                require(context, p, SqlFeature.SIMILAR_TO_PREDICATE, "predicate.like");
            }
            return super.visitLikePredicate(p);
        }

        @Override
        public Void visitRegexPredicate(RegexPredicate p) {
            require(context, p, SqlFeature.REGEX_PREDICATE, "predicate.regex");
            return super.visitRegexPredicate(p);
        }

        @Override
        public Void visitLateral(Lateral i) {
            require(context, i, SqlFeature.LATERAL, "from.lateral");
            return super.visitLateral(i);
        }

        @Override
        public Void visitFunctionTable(FunctionTable t) {
            require(context, t, SqlFeature.FUNCTION_TABLE, "from.function");
            if (t.ordinality()) {
                require(context, t, SqlFeature.FUNCTION_TABLE_ORDINALITY, "from.function");
            }
            return super.visitFunctionTable(t);
        }

        @Override
        public Void visitTable(Table table) {
            if (table.inheritance() == Table.Inheritance.ONLY) {
                require(context, table, SqlFeature.TABLE_INHERITANCE_ONLY, "from.table");
            } else if (table.inheritance() == Table.Inheritance.INCLUDE_DESCENDANTS) {
                require(context, table, SqlFeature.TABLE_INHERITANCE_DESCENDANTS, "from.table");
            }
            return super.visitTable(table);
        }

        @Override
        public Void visitArrayExpr(ArrayExpr expr) {
            require(context, expr, SqlFeature.ARRAY_LITERAL, "expression.array");
            return super.visitArrayExpr(expr);
        }

        @Override
        public Void visitArraySubscriptExpr(ArraySubscriptExpr expr) {
            require(context, expr, SqlFeature.ARRAY_SUBSCRIPT, "expression.array");
            return super.visitArraySubscriptExpr(expr);
        }

        @Override
        public Void visitArraySliceExpr(ArraySliceExpr expr) {
            require(context, expr, SqlFeature.ARRAY_SLICE, "expression.array");
            return super.visitArraySliceExpr(expr);
        }

        @Override
        public Void visitCollateExpr(CollateExpr expr) {
            require(context, expr, SqlFeature.EXPR_COLLATE, "expression.collate");
            return super.visitCollateExpr(expr);
        }

        @Override
        public Void visitAtTimeZoneExpr(AtTimeZoneExpr expr) {
            require(context, expr, SqlFeature.AT_TIME_ZONE, "expression.at_time_zone");
            return super.visitAtTimeZoneExpr(expr);
        }

        @Override
        public Void visitBinaryOperatorExpr(BinaryOperatorExpr expr) {
            require(context, expr, SqlFeature.CUSTOM_OPERATOR, "expression.operator");
            return super.visitBinaryOperatorExpr(expr);
        }

        @Override
        public Void visitCastExpr(CastExpr expr) {
            require(context, expr, SqlFeature.POSTGRES_TYPECAST, "expression.cast");
            return super.visitCastExpr(expr);
        }

        @Override
        public Void visitPowerArithmeticExpr(PowerArithmeticExpr expr) {
            require(context, expr, SqlFeature.EXPONENTIATION_OPERATOR, "expression.power");
            return super.visitPowerArithmeticExpr(expr);
        }

        @Override
        public Void visitGroupingSets(GroupItem.GroupingSets i) {
            require(context, i, SqlFeature.GROUPING_SETS, "select.group_by");
            return super.visitGroupingSets(i);
        }

        @Override
        public Void visitRollup(GroupItem.Rollup i) {
            require(context, i, SqlFeature.ROLLUP, "select.group_by");
            return super.visitRollup(i);
        }

        @Override
        public Void visitCube(GroupItem.Cube i) {
            require(context, i, SqlFeature.CUBE, "select.group_by");
            return super.visitCube(i);
        }

        @Override
        public Void visitIsDistinctFromPredicate(IsDistinctFromPredicate p) {
            require(context, p, SqlFeature.IS_DISTINCT_FROM_PREDICATE, "predicate.is_distinct_from");
            return super.visitIsDistinctFromPredicate(p);
        }

        @Override
        public Void visitOverDef(OverSpec.Def d) {
            if (d.exclude() != null) {
                require(context, d, SqlFeature.WINDOW_FRAME_EXCLUDE, "window.frame");
            }
            return super.visitOverDef(d);
        }

        @Override
        public Void visitFrameSingle(FrameSpec.Single f) {
            if (f.unit() == FrameSpec.Unit.GROUPS) {
                require(context, f, SqlFeature.WINDOW_FRAME_GROUPS, "window.frame");
            }
            return super.visitFrameSingle(f);
        }

        @Override
        public Void visitFrameBetween(FrameSpec.Between f) {
            if (f.unit() == FrameSpec.Unit.GROUPS) {
                require(context, f, SqlFeature.WINDOW_FRAME_GROUPS, "window.frame");
            }
            return super.visitFrameBetween(f);
        }

        /**
         * Skips traversal into nested FROM-subqueries.
         *
         * <p>Nested queries are validated by the outer schema-validation visitor
         * in their own scope lifecycle. Descending here would duplicate feature
         * checks and can blur parent/child scope boundaries.</p>
         */
        @Override
        public Void visitQueryTable(QueryTable t) {
            // Nested query has its own validation cycle.
            return defaultResult();
        }

        /**
         * Skips traversal into scalar subqueries.
         *
         * <p>Scalar subqueries are validated separately as full query nodes.
         * Re-entering them from this local walker would apply checks twice.</p>
         */
        @Override
        public Void visitQueryExpr(QueryExpr v) {
            // Nested subquery has its own validation cycle.
            return defaultResult();
        }

        /**
         * Skips traversal into EXISTS subqueries.
         *
         * <p>The nested query is validated by the main schema-validation visitor
         * as an independent query scope.</p>
         */
        @Override
        public Void visitExistsPredicate(ExistsPredicate p) {
            return defaultResult();
        }

        /**
         * Skips traversal into ANY/ALL subqueries while still walking the left side.
         *
         * <p>The subquery term is validated independently by the main traversal.</p>
         */
        @Override
        public Void visitAnyAllPredicate(AnyAllPredicate p) {
            accept(p.lhs());
            return defaultResult();
        }
    }
}
