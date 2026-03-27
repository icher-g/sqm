package io.sqm.validate.sqlserver.rule;

import io.sqm.core.AtTimeZoneExpr;
import io.sqm.core.ExistsPredicate;
import io.sqm.core.Node;
import io.sqm.core.QueryExpr;
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
 * Validates SQL Server {@code AT TIME ZONE} support against versioned capabilities.
 */
public final class SqlServerExpressionFeatureValidationRule implements SchemaValidationRule<SelectQuery> {
    private final DialectCapabilities capabilities;
    private final SqlDialectVersion version;

    /**
     * Creates a SQL Server expression feature validation rule.
     *
     * @param capabilities dialect capabilities.
     * @param version SQL Server version.
     */
    public SqlServerExpressionFeatureValidationRule(DialectCapabilities capabilities, SqlDialectVersion version) {
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
        this.version = Objects.requireNonNull(version, "version");
    }

    @Override
    public Class<SelectQuery> nodeType() {
        return SelectQuery.class;
    }

    @Override
    public void validate(SelectQuery node, SchemaValidationContext context) {
        new FeatureWalker(context).acceptNode(node);
    }

    private final class FeatureWalker extends RecursiveNodeVisitor<Void> {
        private final SchemaValidationContext context;

        private FeatureWalker(SchemaValidationContext context) {
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
        public Void visitAtTimeZoneExpr(AtTimeZoneExpr expr) {
            if (!capabilities.supports(SqlFeature.AT_TIME_ZONE)) {
                context.addProblem(
                    ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
                    "SQL Server " + version + " does not support " + SqlFeature.AT_TIME_ZONE.description(),
                    expr,
                    "expression.at_time_zone"
                );
            }
            return super.visitAtTimeZoneExpr(expr);
        }

        @Override
        public Void visitQueryExpr(QueryExpr v) {
            return defaultResult();
        }

        @Override
        public Void visitExistsPredicate(ExistsPredicate p) {
            return defaultResult();
        }
    }
}