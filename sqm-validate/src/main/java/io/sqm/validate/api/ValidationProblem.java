package io.sqm.validate.api;

import java.util.Objects;

/**
 * Represents a single semantic validation problem.
 *
 * @param code problem code.
 * @param message detailed human-readable message.
 * @param nodeKind optional node kind where problem occurred.
 * @param clausePath optional clause/path hint (for example {@code where}, {@code join.using}).
 */
public record ValidationProblem(
    ValidationProblem.Code code,
    String message,
    String nodeKind,
    String clausePath
) {
    /**
     * Creates a validation problem without structured context.
     *
     * @param code problem code.
     * @param message detailed message.
     */
    public ValidationProblem(ValidationProblem.Code code, String message) {
        this(code, message, null, null);
    }

    /**
     * Creates a validation problem.
     *
     * @param code problem code.
     * @param message detailed message.
     */
    public ValidationProblem {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
    }

    /**
     * Problem categories returned by semantic validator.
     */
    public enum Code {
        /**
         * Table reference is missing in schema.
         */
        TABLE_NOT_FOUND,
        /**
         * Unqualified table name resolves to multiple tables.
         */
        TABLE_AMBIGUOUS,
        /**
         * Referenced table alias is missing in current scope.
         */
        UNKNOWN_TABLE_ALIAS,
        /**
         * Referenced column is missing.
         */
        COLUMN_NOT_FOUND,
        /**
         * Unqualified column resolves to multiple table sources.
         */
        COLUMN_AMBIGUOUS,
        /**
         * Incompatible data types found in expression comparison.
         */
        TYPE_MISMATCH,
        /**
         * Duplicate table alias in single FROM scope.
         */
        DUPLICATE_TABLE_ALIAS,
        /**
         * USING join contains missing or ambiguous column references.
         */
        JOIN_USING_INVALID_COLUMN,
        /**
         * ON join predicate references aliases that are not visible at this join position.
         */
        JOIN_ON_INVALID_REFERENCE,
        /**
         * ON join predicate is missing for a regular join node.
         */
        JOIN_ON_MISSING_PREDICATE,
        /**
         * ON join predicate contains a non-boolean expression in predicate context.
         */
        JOIN_ON_INVALID_BOOLEAN_EXPRESSION,
        /**
         * IN/NOT IN row-value shape (tuple width) is invalid.
         */
        IN_ROW_SHAPE_MISMATCH,
        /**
         * Subquery shape is invalid for scalar/quantified predicate context.
         */
        SUBQUERY_SHAPE_MISMATCH,
        /**
         * Set operation terms have incompatible column counts.
         */
        SET_OPERATION_COLUMN_COUNT_MISMATCH,
        /**
         * Aggregation context is invalid for SELECT or HAVING.
         */
        AGGREGATION_MISUSE,
        /**
         * Function call does not match known signature constraints.
         */
        FUNCTION_SIGNATURE_MISMATCH,
        /**
         * ORDER BY ordinal points outside available select-item positions.
         */
        ORDER_BY_INVALID_ORDINAL,
        /**
         * GROUP BY ordinal points outside available select-item positions.
         */
        GROUP_BY_INVALID_ORDINAL,
        /**
         * Window reference in OVER clause is not defined in current SELECT.
         */
        WINDOW_NOT_FOUND,
        /**
         * Duplicate window name in WINDOW clause within one SELECT block.
         */
        DUPLICATE_WINDOW_NAME,
        /**
         * Window inheritance graph contains a cycle.
         */
        WINDOW_INHERITANCE_CYCLE,
        /**
         * Window frame specification is invalid.
         */
        WINDOW_FRAME_INVALID,
        /**
         * DISTINCT ON expressions do not match leftmost ORDER BY expressions.
         */
        DISTINCT_ON_ORDER_BY_MISMATCH,
        /**
         * LIMIT/OFFSET expression has invalid type or literal value.
         */
        LIMIT_OFFSET_INVALID,
        /**
         * FOR ... OF lock target is not visible in current SELECT scope.
         */
        LOCK_TARGET_NOT_FOUND,
        /**
         * ORDER BY in set operation references expression outside set-operation output shape.
         */
        SET_OPERATION_ORDER_BY_INVALID,
        /**
         * CTE column alias count does not match CTE body projection width.
         */
        CTE_COLUMN_ALIAS_COUNT_MISMATCH,
        /**
         * Duplicate CTE name in a single WITH block.
         */
        DUPLICATE_CTE_NAME,
        /**
         * Non-recursive WITH contains self-referencing CTE body.
         */
        CTE_RECURSION_NOT_ALLOWED,
        /**
         * Recursive CTE does not follow required anchor/recursive term structure.
         */
        CTE_RECURSIVE_STRUCTURE_INVALID,
        /**
         * Recursive CTE anchor and recursive terms have incompatible projected types.
         */
        CTE_RECURSIVE_TYPE_MISMATCH,
        /**
         * DML statement is not allowed in read-only mode.
         */
        DML_NOT_ALLOWED,
        /**
         * DDL statement is not allowed in read-only mode.
         */
        DDL_NOT_ALLOWED,
        /**
         * Referenced table is denied by access policy.
         */
        POLICY_TABLE_DENIED,
        /**
         * Referenced column is denied by access policy.
         */
        POLICY_COLUMN_DENIED,
        /**
         * Function call is denied by access policy allowlist.
         */
        POLICY_FUNCTION_NOT_ALLOWED,
        /**
         * Query exceeds configured maximum join count.
         */
        POLICY_MAX_JOINS_EXCEEDED,
        /**
         * Query exceeds configured maximum projected column count.
         */
        POLICY_MAX_SELECT_COLUMNS_EXCEEDED,
        /**
         * Query uses a feature not supported by the configured SQL dialect/version.
         */
        DIALECT_FEATURE_UNSUPPORTED,
        /**
         * Query uses an invalid dialect-specific clause combination.
         */
        DIALECT_CLAUSE_INVALID
    }
}
