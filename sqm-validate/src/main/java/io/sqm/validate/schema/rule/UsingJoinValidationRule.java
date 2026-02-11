package io.sqm.validate.schema.rule;

import io.sqm.core.UsingJoin;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.model.DbType;

import java.util.Optional;

/**
 * Validates USING join column existence and compatibility.
 */
final class UsingJoinValidationRule implements SchemaValidationRule<UsingJoin> {
    /**
     * Returns supported node type.
     *
     * @return USING join type.
     */
    @Override
    public Class<UsingJoin> nodeType() {
        return UsingJoin.class;
    }

    /**
     * Validates USING columns against left and right join sides.
     *
     * @param node USING join node.
     * @param context schema validation context.
     */
    @Override
    public void validate(UsingJoin node, SchemaValidationContext context) {
        var rightSourceKey = context.sourceKey(node.right()).orElse(null);
        for (var column : node.usingColumns()) {
            var leftMatches = context.countStrictSourcesWithColumn(column, rightSourceKey);
            var rightType = rightSourceKey == null ? Optional.<DbType>empty() : context.sourceColumnType(rightSourceKey, column);

            if (leftMatches == 0 || rightType.isEmpty()) {
                context.addProblem(
                    ValidationProblem.Code.JOIN_USING_INVALID_COLUMN,
                    "USING column must exist on both sides: " + column,
                    node,
                    "join.using"
                );
                continue;
            }
            if (leftMatches > 1) {
                context.addProblem(
                    ValidationProblem.Code.JOIN_USING_INVALID_COLUMN,
                    "USING column is ambiguous on left side: " + column,
                    node,
                    "join.using"
                );
                continue;
            }

            // Resolve unique left-side type by scanning strict sources excluding right.
            DbType leftType = null;
            for (var sourceKey : context.currentScopeSourceKeys()) {
                if (rightSourceKey != null && rightSourceKey.equals(sourceKey)) {
                    continue;
                }
                var candidate = context.sourceColumnType(sourceKey, column);
                if (candidate.isPresent()) {
                    leftType = candidate.get();
                    break;
                }
            }

            if (leftType != null && !DbType.comparable(leftType, rightType.get())) {
                context.addProblem(
                    ValidationProblem.Code.TYPE_MISMATCH,
                    "Incompatible USING column types for '" + column + "': " + leftType + " and " + rightType.get(),
                    node,
                    "join.using"
                );
            }
        }
    }
}
