package io.sqm.validate.mysql.rule;

import io.sqm.core.AnyAllPredicate;
import io.sqm.core.ExistsPredicate;
import io.sqm.core.FunctionTable;
import io.sqm.core.Lateral;
import io.sqm.core.Node;
import io.sqm.core.QueryExpr;
import io.sqm.core.QueryTable;
import io.sqm.core.SelectQuery;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.walk.RecursiveNodeVisitor;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.rule.SchemaValidationRule;

import java.util.Objects;

/**
 * Validates MySQL {@code SELECT}-level feature usage against versioned
 * dialect capabilities.
 */
public final class MySqlSelectFeatureValidationRule implements SchemaValidationRule<SelectQuery> {
    private final DialectCapabilities capabilities;
    private final SqlDialectVersion version;

    /**
     * Creates a MySQL SELECT feature validation rule.
     *
     * @param capabilities dialect capabilities.
     * @param version MySQL version.
     */
    public MySqlSelectFeatureValidationRule(DialectCapabilities capabilities, SqlDialectVersion version) {
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
        this.version = Objects.requireNonNull(version, "version");
    }

    @Override
    public Class<SelectQuery> nodeType() {
        return SelectQuery.class;
    }

    @Override
    public void validate(SelectQuery node, SchemaValidationContext context) {
        new SelectFeatureWalker(context).acceptNode(node);
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
            "MySQL " + version + " does not support " + feature.description(),
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
        public Void visitLateral(Lateral lateral) {
            require(context, lateral, SqlFeature.LATERAL, "from.lateral");
            if (!(lateral.inner() instanceof QueryTable queryTable)) {
                context.addProblem(
                    ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                    "MySQL LATERAL supports only derived tables",
                    lateral,
                    "from.lateral"
                );
                return defaultResult();
            }
            if (queryTable.alias() == null) {
                context.addProblem(
                    ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                    "MySQL LATERAL derived tables require an alias",
                    lateral,
                    "from.lateral"
                );
            }
            return defaultResult();
        }

        @Override
        public Void visitFunctionTable(FunctionTable table) {
            require(context, table, SqlFeature.FUNCTION_TABLE, "from.function_table");
            if (table.ordinality()) {
                require(context, table, SqlFeature.FUNCTION_TABLE_ORDINALITY, "from.function_table");
            }
            return defaultResult();
        }

        /**
         * Skips traversal into nested FROM-subqueries.
         *
         * <p>Nested queries are validated by the outer schema-validation visitor
         * in their own scope lifecycle.</p>
         */
        @Override
        public Void visitQueryTable(QueryTable t) {
            return defaultResult();
        }

        /**
         * Skips traversal into scalar subqueries.
         *
         * <p>The nested query is validated separately as a full query node.</p>
         */
        @Override
        public Void visitQueryExpr(QueryExpr v) {
            return defaultResult();
        }

        /**
         * Skips traversal into EXISTS subqueries.
         *
         * <p>The nested query is validated independently by the main traversal.</p>
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
