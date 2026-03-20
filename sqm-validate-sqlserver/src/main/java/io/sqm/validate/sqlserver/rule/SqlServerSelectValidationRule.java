package io.sqm.validate.sqlserver.rule;

import io.sqm.core.*;
import io.sqm.core.walk.RecursiveNodeVisitor;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.rule.SchemaValidationRule;

/**
 * Validates SQL Server-specific SELECT baseline constraints.
 */
public final class SqlServerSelectValidationRule implements SchemaValidationRule<SelectQuery> {

    /**
     * Creates a SQL Server select validation rule.
     */
    public SqlServerSelectValidationRule() {
    }

    @Override
    public Class<SelectQuery> nodeType() {
        return SelectQuery.class;
    }

    /**
     * Validates SQL Server query shapes that are unsupported or invalid in the
     * current baseline support slice.
     *
     * @param node    select query.
     * @param context validation context.
     */
    @Override
    public void validate(SelectQuery node, SchemaValidationContext context) {
        validateTop(node, context);
        validateLimitOffset(node, context);

        if (node.lockFor() != null) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
                "SQL Server baseline support does not include SELECT locking clauses",
                node.lockFor(),
                "select.lock"
            );
        }

        if (!node.optimizerHints().isEmpty()) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
                "SQL Server baseline support does not include optimizer hint comments",
                node,
                "select.hint"
            );
        }

        if (!node.modifiers().isEmpty()) {
            for (SelectModifier modifier : node.modifiers()) {
                context.addProblem(
                    ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
                    "SQL Server does not support SELECT modifier " + modifier,
                    node,
                    "select.modifier"
                );
            }
        }

        new SelectFeatureWalker(context).acceptNode(node);
    }

    private void validateTop(SelectQuery node, SchemaValidationContext context) {
        var topSpec = node.topSpec();
        if (topSpec == null) {
            return;
        }
        if (topSpec.withTies() && node.orderBy() == null) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                "SQL Server TOP WITH TIES requires ORDER BY",
                topSpec,
                "select.top"
            );
        }
    }

    private void validateLimitOffset(SelectQuery node, SchemaValidationContext context) {
        LimitOffset limitOffset = node.limitOffset();
        if (limitOffset == null) {
            return;
        }

        var hasLimit = limitOffset.limit() != null;
        var hasOffset = limitOffset.offset() != null;
        var hasPagination = hasLimit || hasOffset || limitOffset.limitAll();

        if (!hasPagination) {
            return;
        }

        if (node.topSpec() != null) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                "SQL Server cannot combine TOP with OFFSET/FETCH",
                node,
                "limit_offset"
            );
        }

        if (limitOffset.limitAll()) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
                "SQL Server does not support LIMIT ALL",
                node,
                "limit_offset"
            );
        }

        if (hasLimit && !hasOffset) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                "SQL Server limit-only queries must use TOP instead of LIMIT/OFFSET",
                node,
                "limit_offset"
            );
        }

        if (hasOffset && node.orderBy() == null) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                "SQL Server OFFSET/FETCH requires ORDER BY",
                node,
                "limit_offset"
            );
        }
    }

    private static final class SelectFeatureWalker extends RecursiveNodeVisitor<Void> {
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
                context.addProblem(
                    ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
                    "SQL Server does not support ORDER BY USING operators",
                    i,
                    "select.order"
                );
            }
            return super.visitOrderItem(i);
        }

        @Override
        public Void visitTable(Table table) {
            if (!table.indexHints().isEmpty()) {
                context.addProblem(
                    ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
                    "SQL Server does not support MySQL index hints",
                    table,
                    "from.table"
                );
            }
            SqlServerTableHintSupport.validateHints(table, context, "from.table");
            if (table.inheritance() != Table.Inheritance.DEFAULT) {
                context.addProblem(
                    ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
                    "SQL Server does not support PostgreSQL table inheritance modifiers",
                    table,
                    "from.table"
                );
            }
            return super.visitTable(table);
        }
    }
}
