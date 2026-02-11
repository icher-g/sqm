package io.sqm.validate.schema.rule;

import io.sqm.core.*;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.model.DbType;

/**
 * Validates type compatibility for IN / NOT IN predicates.
 */
final class InPredicateTypeValidationRule implements SchemaValidationRule<InPredicate> {
    private final ProjectionShapeInspector projectionShapeInspector;

    /**
     * Creates IN predicate validation rule.
     *
     * @param projectionShapeInspector projection shape inspector.
     */
    InPredicateTypeValidationRule(ProjectionShapeInspector projectionShapeInspector) {
        this.projectionShapeInspector = projectionShapeInspector;
    }

    /**
     * Returns supported node type.
     *
     * @return IN predicate type.
     */
    @Override
    public Class<InPredicate> nodeType() {
        return InPredicate.class;
    }

    /**
     * Validates scalar IN comparability for row-value and subquery value sets.
     *
     * @param node IN predicate node.
     * @param context schema validation context.
     */
    @Override
    public void validate(InPredicate node, SchemaValidationContext context) {
        if (node.lhs() instanceof RowExpr leftRow) {
            validateRowValueIn(leftRow, node.rhs(), node, context, projectionShapeInspector);
            return;
        }
        var leftType = context.inferType(node.lhs());
        if (leftType.isEmpty()) {
            return;
        }

        switch (node.rhs()) {
            case QueryExpr queryExpr -> validateAgainstQuery(leftType.get(), queryExpr, node, context, projectionShapeInspector);
            case RowExpr rowExpr -> validateAgainstScalarRow(leftType.get(), rowExpr, node, context);
            case RowListExpr rowListExpr -> validateAgainstScalarRowList(leftType.get(), rowListExpr, node, context);
            default -> {
            }
        }
    }

    /**
     * Validates scalar IN against subquery output type.
     *
     * @param leftType left-hand type.
     * @param queryExpr query expression value set.
     * @param context schema validation context.
     */
    private static void validateAgainstQuery(
        DbType leftType,
        QueryExpr queryExpr,
        InPredicate node,
        SchemaValidationContext context,
        ProjectionShapeInspector projectionShapeInspector
    ) {
        if (!projectionShapeInspector.isSingleExpressionProjection(queryExpr.subquery())) {
            context.addProblem(
                ValidationProblem.Code.SUBQUERY_SHAPE_MISMATCH,
                "IN subquery must project exactly one expression column",
                node,
                "predicate.in"
            );
            return;
        }
        var rightType = context.inferSingleColumnType(queryExpr.subquery());
        if (rightType.isPresent() && !DbType.comparable(leftType, rightType.get())) {
            context.addProblem(
                ValidationProblem.Code.TYPE_MISMATCH,
                "Incompatible IN types: " + leftType + " and " + rightType.get(),
                node,
                "predicate.in"
            );
        }
    }

    /**
     * Validates scalar IN against explicit scalar value list represented by a row.
     *
     * @param leftType left-hand type.
     * @param rowExpr row expression value set.
     * @param context schema validation context.
     */
    private static void validateAgainstScalarRow(
        DbType leftType,
        RowExpr rowExpr,
        InPredicate node,
        SchemaValidationContext context
    ) {
        for (var item : rowExpr.items()) {
            var rightType = context.inferType(item);
            if (rightType.isPresent() && !DbType.comparable(leftType, rightType.get())) {
                context.addProblem(
                    ValidationProblem.Code.TYPE_MISMATCH,
                    "Incompatible IN types: " + leftType + " and " + rightType.get(),
                    node,
                    "predicate.in"
                );
            }
        }
    }

    /**
     * Validates scalar IN against row-list value set when rows are single-column.
     *
     * @param leftType left-hand type.
     * @param rowListExpr row-list value set.
     * @param context schema validation context.
     */
    private static void validateAgainstScalarRowList(
        DbType leftType,
        RowListExpr rowListExpr,
        InPredicate node,
        SchemaValidationContext context
    ) {
        for (var row : rowListExpr.rows()) {
            if (row.items().size() != 1) {
                reportShapeMismatch(
                    node,
                    "Scalar IN expects single-column values but RHS row has width " + row.items().size(),
                    context
                );
                continue;
            }
            var rightType = context.inferType(row.items().getFirst());
            if (rightType.isPresent() && !DbType.comparable(leftType, rightType.get())) {
                context.addProblem(
                    ValidationProblem.Code.TYPE_MISMATCH,
                    "Incompatible IN types: " + leftType + " and " + rightType.get(),
                    node,
                    "predicate.in"
                );
            }
        }
    }

    /**
     * Validates row-value IN tuple width and per-column comparability.
     *
     * @param leftRow left-hand row expression.
     * @param rhs right-hand value set.
     * @param node IN predicate node.
     * @param context validation context.
     * @param projectionShapeInspector projection shape inspector.
     */
    private static void validateRowValueIn(
        RowExpr leftRow,
        ValueSet rhs,
        InPredicate node,
        SchemaValidationContext context,
        ProjectionShapeInspector projectionShapeInspector
    ) {
        var leftWidth = leftRow.items().size();
        switch (rhs) {
            case QueryExpr queryExpr -> validateRowAgainstQuery(leftRow, leftWidth, queryExpr, node, context, projectionShapeInspector);
            case RowExpr rowExpr -> validateRowAgainstRow(leftRow, leftWidth, rowExpr, node, context);
            case RowListExpr rowListExpr -> validateRowAgainstRowList(leftRow, leftWidth, rowListExpr, node, context);
            default -> {
            }
        }
    }

    /**
     * Validates row-value IN against subquery output shape and types.
     *
     * @param leftRow left-hand row expression.
     * @param leftWidth left tuple width.
     * @param queryExpr subquery value set.
     * @param node IN predicate node.
     * @param context validation context.
     * @param projectionShapeInspector projection shape inspector.
     */
    private static void validateRowAgainstQuery(
        RowExpr leftRow,
        int leftWidth,
        QueryExpr queryExpr,
        InPredicate node,
        SchemaValidationContext context,
        ProjectionShapeInspector projectionShapeInspector
    ) {
        var projectionArity = projectionShapeInspector.projectionArity(queryExpr.subquery());
        if (projectionArity.isEmpty()) {
            return;
        }
        if (projectionArity.get() != leftWidth) {
            reportShapeMismatch(
                node,
                "Row-value IN subquery width "
                    + projectionArity.get()
                    + " does not match left tuple width "
                    + leftWidth,
                context
            );
            return;
        }
        var projectionTypes = projectionShapeInspector.expressionProjectionTypes(queryExpr.subquery(), context);
        if (projectionTypes.isEmpty()) {
            return;
        }
        validateTupleTypesAgainstProjectionTypes(leftRow.items(), projectionTypes.get(), node, context);
    }

    /**
     * Validates row-value IN against one explicit RHS row.
     *
     * @param leftRow left-hand row expression.
     * @param leftWidth left tuple width.
     * @param rowExpr row value set.
     * @param node IN predicate node.
     * @param context validation context.
     */
    private static void validateRowAgainstRow(
        RowExpr leftRow,
        int leftWidth,
        RowExpr rowExpr,
        InPredicate node,
        SchemaValidationContext context
    ) {
        if (rowExpr.items().size() != leftWidth) {
            reportShapeMismatch(
                node,
                "Row-value IN RHS tuple width "
                    + rowExpr.items().size()
                    + " does not match left tuple width "
                    + leftWidth,
                context
            );
            return;
        }
        validateTupleTypes(leftRow.items(), rowExpr.items(), node, context);
    }

    /**
     * Validates row-value IN against RHS row list.
     *
     * @param leftRow left-hand row expression.
     * @param leftWidth left tuple width.
     * @param rowListExpr row-list value set.
     * @param node IN predicate node.
     * @param context validation context.
     */
    private static void validateRowAgainstRowList(
        RowExpr leftRow,
        int leftWidth,
        RowListExpr rowListExpr,
        InPredicate node,
        SchemaValidationContext context
    ) {
        for (var row : rowListExpr.rows()) {
            if (row.items().size() != leftWidth) {
                reportShapeMismatch(
                    node,
                    "Row-value IN RHS tuple width "
                        + row.items().size()
                        + " does not match left tuple width "
                        + leftWidth,
                    context
                );
                continue;
            }
            validateTupleTypes(leftRow.items(), row.items(), node, context);
        }
    }

    /**
     * Validates comparability for two tuples represented by expression lists.
     *
     * @param leftItems left tuple expressions.
     * @param rightItems right tuple expressions.
     * @param node IN predicate node.
     * @param context validation context.
     */
    private static void validateTupleTypes(
        java.util.List<Expression> leftItems,
        java.util.List<Expression> rightItems,
        InPredicate node,
        SchemaValidationContext context
    ) {
        var width = Math.min(leftItems.size(), rightItems.size());
        for (int i = 0; i < width; i++) {
            var leftType = context.inferType(leftItems.get(i));
            var rightType = context.inferType(rightItems.get(i));
            if (leftType.isPresent() && rightType.isPresent() && !DbType.comparable(leftType.get(), rightType.get())) {
                context.addProblem(
                    ValidationProblem.Code.TYPE_MISMATCH,
                    "Incompatible IN tuple types at column " + (i + 1) + ": " + leftType.get() + " and " + rightType.get(),
                    node,
                    "predicate.in"
                );
            }
        }
    }

    /**
     * Validates comparability for tuple expression list and projected tuple types.
     *
     * @param leftItems left tuple expressions.
     * @param rightTypes projected right tuple types.
     * @param node IN predicate node.
     * @param context validation context.
     */
    private static void validateTupleTypesAgainstProjectionTypes(
        java.util.List<Expression> leftItems,
        java.util.List<java.util.Optional<DbType>> rightTypes,
        InPredicate node,
        SchemaValidationContext context
    ) {
        var width = Math.min(leftItems.size(), rightTypes.size());
        for (int i = 0; i < width; i++) {
            var leftType = context.inferType(leftItems.get(i));
            var rightType = rightTypes.get(i);
            if (leftType.isPresent() && rightType.isPresent() && !DbType.comparable(leftType.get(), rightType.get())) {
                context.addProblem(
                    ValidationProblem.Code.TYPE_MISMATCH,
                    "Incompatible IN tuple types at column " + (i + 1) + ": " + leftType.get() + " and " + rightType.get(),
                    node,
                    "predicate.in"
                );
            }
        }
    }

    /**
     * Reports row-value IN shape mismatch.
     *
     * @param node IN predicate node.
     * @param message diagnostic message.
     * @param context validation context.
     */
    private static void reportShapeMismatch(
        InPredicate node,
        String message,
        SchemaValidationContext context
    ) {
        context.addProblem(
            ValidationProblem.Code.IN_ROW_SHAPE_MISMATCH,
            message,
            node,
            "predicate.in"
        );
    }
}

