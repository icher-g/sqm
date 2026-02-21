package io.sqm.control;

import io.sqm.core.Query;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.api.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultSqlDecisionEngineTest {

    @Test
    void allows_when_validation_passes() {
        var engine = DefaultSqlDecisionEngine.of(query -> new ValidationResult(List.of()));
        var result = engine.evaluate(Query.select(io.sqm.core.Expression.literal(1)), ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));

        assertEquals(DecisionKind.ALLOW, result.kind());
        assertEquals(ReasonCode.NONE, result.reasonCode());
    }

    @Test
    void maps_policy_errors_to_reason_codes() {
        var engine = DefaultSqlDecisionEngine.of(query -> new ValidationResult(List.of(
            new ValidationProblem(ValidationProblem.Code.POLICY_TABLE_DENIED, "table denied")
        )));

        var result = engine.evaluate(Query.select(io.sqm.core.Expression.literal(1)), ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));
        assertEquals(DecisionKind.DENY, result.kind());
        assertEquals(ReasonCode.DENY_TABLE, result.reasonCode());
    }

    @Test
    void maps_generic_validation_to_deny_validation() {
        var engine = DefaultSqlDecisionEngine.of(query -> new ValidationResult(List.of(
            new ValidationProblem(ValidationProblem.Code.TYPE_MISMATCH, "type mismatch")
        )));

        var result = engine.evaluate(Query.select(io.sqm.core.Expression.literal(1)), ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));
        assertEquals(DecisionKind.DENY, result.kind());
        assertEquals(ReasonCode.DENY_VALIDATION, result.reasonCode());
    }
}

