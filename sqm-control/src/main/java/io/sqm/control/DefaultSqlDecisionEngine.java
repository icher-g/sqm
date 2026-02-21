package io.sqm.control;

import io.sqm.core.Query;
import io.sqm.validate.api.QueryValidator;
import io.sqm.validate.api.ValidationProblem;

import java.util.Objects;

/**
 * Default decision engine that delegates semantic checks to {@link QueryValidator}.
 */
public final class DefaultSqlDecisionEngine implements SqlDecisionEngine {
    private final QueryValidator queryValidator;

    private DefaultSqlDecisionEngine(QueryValidator queryValidator) {
        this.queryValidator = queryValidator;
    }

    /**
     * Creates a decision engine backed by a query validator.
     *
     * @param queryValidator semantic validator
     * @return engine instance
     */
    public static DefaultSqlDecisionEngine of(QueryValidator queryValidator) {
        Objects.requireNonNull(queryValidator, "queryValidator must not be null");
        return new DefaultSqlDecisionEngine(queryValidator);
    }

    /**
     * Evaluates a query model and maps validation outcomes to middleware decisions.
     *
     * @param query query model
     * @param context execution context
     * @return decision result
     */
    @Override
    public DecisionResult evaluate(Query query, ExecutionContext context) {
        Objects.requireNonNull(query, "query must not be null");
        Objects.requireNonNull(context, "context must not be null");

        var validation = queryValidator.validate(query);
        if (validation.ok()) {
            return DecisionResult.allow();
        }

        var first = validation.problems().getFirst();
        return DecisionResult.deny(mapReason(first.code()), first.message());
    }

    private static ReasonCode mapReason(ValidationProblem.Code code) {
        return switch (code) {
            case DDL_NOT_ALLOWED -> ReasonCode.DENY_DDL;
            case DML_NOT_ALLOWED -> ReasonCode.DENY_DML;
            case POLICY_TABLE_DENIED -> ReasonCode.DENY_TABLE;
            case POLICY_COLUMN_DENIED -> ReasonCode.DENY_COLUMN;
            case POLICY_FUNCTION_NOT_ALLOWED -> ReasonCode.DENY_FUNCTION;
            case POLICY_MAX_JOINS_EXCEEDED -> ReasonCode.DENY_MAX_JOINS;
            case POLICY_MAX_SELECT_COLUMNS_EXCEEDED -> ReasonCode.DENY_MAX_SELECT_COLUMNS;
            case DIALECT_FEATURE_UNSUPPORTED, DIALECT_CLAUSE_INVALID -> ReasonCode.DENY_UNSUPPORTED_DIALECT_FEATURE;
            default -> ReasonCode.DENY_VALIDATION;
        };
    }
}

