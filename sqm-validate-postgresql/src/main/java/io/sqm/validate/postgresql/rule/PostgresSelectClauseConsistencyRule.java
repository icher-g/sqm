package io.sqm.validate.postgresql.rule;

import io.sqm.core.*;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.walk.RecursiveNodeVisitor;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.rule.SchemaValidationRule;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Validates PostgreSQL clause-level semantic consistency that is inferable from AST only.
 */
public final class PostgresSelectClauseConsistencyRule implements SchemaValidationRule<SelectQuery> {
    private final DialectCapabilities capabilities;

    /**
     * Creates PostgreSQL clause-consistency rule.
     *
     * @param capabilities dialect capabilities for version-aware checks.
     */
    public PostgresSelectClauseConsistencyRule(DialectCapabilities capabilities) {
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
    }

    @Override
    public Class<SelectQuery> nodeType() {
        return SelectQuery.class;
    }

    @Override
    public void validate(SelectQuery node, SchemaValidationContext context) {
        validateLockingContext(node, context);
        validateLockTargets(node, context);
        new ClauseWalker(context).acceptNode(node);
    }

    private static void validateLockingContext(SelectQuery node, SchemaValidationContext context) {
        if (node.lockFor() == null) {
            return;
        }
        if (node.from() == null) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                "PostgreSQL does not allow FOR ... locking without FROM",
                node.lockFor(),
                "select.lock"
            );
        }
        if (node.distinct() != null) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                "PostgreSQL does not allow FOR ... locking with DISTINCT",
                node.lockFor(),
                "select.lock"
            );
        }
        if (node.groupBy() != null && !node.groupBy().items().isEmpty()) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                "PostgreSQL does not allow FOR ... locking with GROUP BY",
                node.lockFor(),
                "select.lock"
            );
        }
        if (node.having() != null) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                "PostgreSQL does not allow FOR ... locking with HAVING",
                node.lockFor(),
                "select.lock"
            );
        }
        if (!node.windows().isEmpty()) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                "PostgreSQL does not allow FOR ... locking with WINDOW clause",
                node.lockFor(),
                "select.lock"
            );
        }
    }

    private static void validateLockTargets(SelectQuery node, SchemaValidationContext context) {
        if (node.lockFor() == null || node.lockFor().ofTables().isEmpty()) {
            return;
        }
        validateDuplicateLockTargets(node, context);
        validateOuterJoinNullableLockTargets(node, context);
    }

    private static void validateDuplicateLockTargets(SelectQuery node, SchemaValidationContext context) {
        var seen = new HashSet<String>();
        for (var target : node.lockFor().ofTables()) {
            if (target == null || target.identifier() == null) {
                continue;
            }
            var normalized = normalize(target.identifier());
            if (seen.add(normalized)) {
                continue;
            }
            context.addProblem(
                ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                "PostgreSQL does not allow duplicate FOR ... OF lock targets: " + target.identifier(),
                node.lockFor(),
                "select.lock"
            );
        }
    }

    private static void validateOuterJoinNullableLockTargets(SelectQuery node, SchemaValidationContext context) {
        var nullableSourceKeys = collectOuterJoinNullableSourceKeys(node, context);
        if (nullableSourceKeys.isEmpty()) {
            return;
        }
        for (var target : node.lockFor().ofTables()) {
            if (target == null || target.identifier() == null) {
                continue;
            }
            if (!nullableSourceKeys.contains(normalize(target.identifier()))) {
                continue;
            }
            context.addProblem(
                ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                "PostgreSQL does not allow FOR ... OF on nullable outer-join side: " + target.identifier(),
                node.lockFor(),
                "select.lock"
            );
        }
    }

    private static Set<String> collectOuterJoinNullableSourceKeys(SelectQuery node, SchemaValidationContext context) {
        var nullable = new LinkedHashSet<String>();
        var leftChain = new LinkedHashSet<String>();
        addSourceKey(node.from(), context, leftChain);

        for (var join : node.joins()) {
            switch (join) {
                case OnJoin onJoin -> markNullableByKind(onJoin.kind(), onJoin.right(), leftChain, nullable, context);
                case UsingJoin usingJoin ->
                    markNullableByKind(usingJoin.kind(), usingJoin.right(), leftChain, nullable, context);
                default -> {
                }
            }
            addSourceKey(join.right(), context, leftChain);
        }
        return nullable;
    }

    private static void markNullableByKind(
        JoinKind kind,
        TableRef right,
        Set<String> leftChain,
        Set<String> nullable,
        SchemaValidationContext context
    ) {
        if (kind == JoinKind.LEFT) {
            addSourceKey(right, context, nullable);
            return;
        }
        if (kind == JoinKind.RIGHT) {
            nullable.addAll(leftChain);
            return;
        }
        if (kind == JoinKind.FULL) {
            nullable.addAll(leftChain);
            addSourceKey(right, context, nullable);
        }
    }

    private static void addSourceKey(TableRef ref, SchemaValidationContext context, Set<String> target) {
        Optional<String> sourceKey = context.sourceKey(ref);
        sourceKey.ifPresent(target::add);
    }

    private static String normalize(String identifier) {
        return identifier.toLowerCase(Locale.ROOT);
    }

    private final class ClauseWalker extends RecursiveNodeVisitor<Void> {
        private final SchemaValidationContext context;

        private ClauseWalker(SchemaValidationContext context) {
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
            if (i.usingOperator() != null && i.usingOperator().isBlank()) {
                context.addProblem(
                    ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                    "PostgreSQL ORDER BY ... USING requires a non-blank operator",
                    i,
                    "select.order"
                );
            }
            if (i.usingOperator() != null && i.direction() != null) {
                context.addProblem(
                    ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                    "PostgreSQL ORDER BY ... USING cannot be combined with ASC/DESC",
                    i,
                    "select.order"
                );
            }
            return super.visitOrderItem(i);
        }

        /**
         * Validates lateral wrapper usage in current query scope.
         *
         * <p>Traversal still descends into the wrapped item, but this method
         * adds PostgreSQL-specific clause consistency checks for the wrapper itself.</p>
         */
        @Override
        public Void visitLateral(Lateral i) {
            if (capabilities.supports(SqlFeature.LATERAL) && i.inner() instanceof Lateral) {
                context.addProblem(
                    ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                    "PostgreSQL does not allow nested LATERAL wrappers",
                    i,
                    "from.lateral"
                );
            }
            if (capabilities.supports(SqlFeature.LATERAL) && i.inner() instanceof Table) {
                context.addProblem(
                    ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                    "PostgreSQL LATERAL applies to derived tables/functions, not base tables",
                    i,
                    "from.lateral"
                );
            }
            return super.visitLateral(i);
        }

        /**
         * Skips traversal into nested FROM-subqueries.
         *
         * <p>Nested SELECT blocks are validated in their own schema-validation cycle.
         * Descending here would duplicate clause checks and blur scope boundaries.</p>
         */
        @Override
        public Void visitQueryTable(QueryTable t) {
            return defaultResult();
        }

        /**
         * Skips traversal into scalar subqueries.
         *
         * <p>Scalar subqueries are validated independently as query nodes by the
         * main traversal lifecycle.</p>
         */
        @Override
        public Void visitQueryExpr(QueryExpr v) {
            return defaultResult();
        }

        /**
         * Skips traversal into EXISTS subqueries.
         *
         * <p>The nested query is validated separately in its own scope.</p>
         */
        @Override
        public Void visitExistsPredicate(ExistsPredicate p) {
            return defaultResult();
        }

        /**
         * Skips traversal into ANY/ALL subqueries while still visiting the left-hand expression.
         *
         * <p>The quantified subquery is validated independently by the main query traversal.</p>
         */
        @Override
        public Void visitAnyAllPredicate(AnyAllPredicate p) {
            accept(p.lhs());
            return defaultResult();
        }
    }
}
