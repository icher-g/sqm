package io.sqm.control;

import io.sqm.core.Expression;
import io.sqm.core.Query;
import io.sqm.control.rewrite.TenantPredicateRewriteRule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.select;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlDecisionEngineTest {

    @Test
    void factory_validates_required_dependencies() {
        var validator = (SqlQueryValidator) (query, context) -> QueryValidateResult.ok();
        var rewriter = SqlQueryRewriter.noop();
        var renderer = (SqlQueryRenderer) (query, context) -> QueryRenderResult.of("select 1");

        assertThrows(NullPointerException.class, () -> SqlDecisionEngine.of(null, rewriter, renderer));
        assertThrows(NullPointerException.class, () -> SqlDecisionEngine.of(validator, null, renderer));
        assertThrows(NullPointerException.class, () -> SqlDecisionEngine.of(validator, rewriter, null));
    }

    @Test
    void evaluate_returns_deny_when_rewritten_query_fails_validation() {
        var query = Query.select(Expression.literal(1)).build();
        var rewritten = Query.select(Expression.literal(2)).build();
        var validatorCall = new int[]{0};

        var engine = SqlDecisionEngine.of(
            (q, context) -> {
                validatorCall[0]++;
                if (validatorCall[0] == 1) {
                    return QueryValidateResult.ok();
                }
                return QueryValidateResult.failure(ReasonCode.DENY_MAX_SELECT_COLUMNS, "too many columns");
            },
            (q, context) -> QueryRewriteResult.rewritten(rewritten, "r1", ReasonCode.REWRITE_LIMIT),
            (q, context) -> QueryRenderResult.of("select 2")
        );

        var result = engine.evaluate(query, ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));

        assertEquals(DecisionKind.DENY, result.kind());
        assertEquals(ReasonCode.DENY_MAX_SELECT_COLUMNS, result.reasonCode());
        assertEquals("too many columns", result.message());
        assertEquals(2, validatorCall[0]);
    }

    @Test
    void evaluate_propagates_rendered_sql_and_params_for_rewrite() {
        var input = Query.select(Expression.literal(1)).build();
        var rewritten = Query.select(Expression.literal(2)).build();

        var engine = SqlDecisionEngine.of(
            (q, context) -> QueryValidateResult.ok(),
            (q, context) -> QueryRewriteResult.rewritten(rewritten, "limit-injection", ReasonCode.REWRITE_LIMIT),
            (q, context) -> QueryRenderResult.of("select ?", List.of(2L))
        );

        var result = engine.evaluate(
            input,
            ExecutionContext.of("postgresql", "alice", "tenant-a", ExecutionMode.ANALYZE, ParameterizationMode.BIND)
        );

        assertEquals(DecisionKind.REWRITE, result.kind());
        assertEquals(ReasonCode.REWRITE_LIMIT, result.reasonCode());
        assertEquals("select ?", result.rewrittenSql());
        assertEquals(List.of(2L), result.sqlParams());
        assertTrue(result.message().contains("limit-injection"));
    }

    @Test
    void evaluate_with_tenant_rewrite_emits_bind_params_in_deterministic_order() {
        var input = select(col("u", "id"))
            .from(tbl("public", "users").as("u"))
            .where(col("u", "id").eq(lit(7)))
            .build();

        var settings = BuiltInRewriteSettings.builder()
            .tenantTablePolicy("public.users", TenantRewriteTablePolicy.required("tenant_id"))
            .build();

        var engine = SqlDecisionEngine.of(
            (q, context) -> QueryValidateResult.ok(),
            SqlQueryRewriter.chain(TenantPredicateRewriteRule.of(settings)),
            SqlQueryRenderer.standard()
        );

        var result = engine.evaluate(
            input,
            ExecutionContext.of("postgresql", null, "tenant_a", ExecutionMode.ANALYZE, ParameterizationMode.BIND)
        );

        assertEquals(DecisionKind.REWRITE, result.kind());
        assertEquals(ReasonCode.REWRITE_TENANT_PREDICATE, result.reasonCode());
        assertTrue(result.rewrittenSql().contains("?"));
        assertEquals(2, result.sqlParams().size());
        assertEquals(7L, ((Number) result.sqlParams().getFirst()).longValue());
        assertEquals("tenant_a", result.sqlParams().get(1));
    }
}
