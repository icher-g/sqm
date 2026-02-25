package io.sqm.control;

import io.sqm.control.rewrite.LimitInjectionRewriteRule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimitInjectionRewriteRuleTest {
    private static final ExecutionContext PG_ANALYZE = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);

    @Test
    void id_and_null_arguments_are_validated() {
        var rule = LimitInjectionRewriteRule.of(10);
        var query = SqlQueryParser.standard().parse("select 1", PG_ANALYZE);

        assertEquals("limit-injection", rule.id());
        assertThrows(NullPointerException.class, () -> rule.apply(null, PG_ANALYZE));
        assertThrows(NullPointerException.class, () -> rule.apply(query, null));
        assertThrows(NullPointerException.class, () -> LimitInjectionRewriteRule.of(null));
    }

    @Test
    void deny_mode_rejects_non_literal_limit_expression() {
        var query = SqlQueryParser.standard().parse("select 1 limit (1 + 1)", PG_ANALYZE);
        var rule = LimitInjectionRewriteRule.of(
            new BuiltInRewriteSettings(10, 10, LimitExcessMode.DENY)
        );

        var ex = assertThrows(RewriteDenyException.class, () -> rule.apply(query, PG_ANALYZE));
        assertEquals(ReasonCode.DENY_MAX_ROWS, ex.reasonCode());
        assertTrue(ex.getMessage().contains("literal value"));
    }

    @Test
    void deny_mode_rejects_limit_all() {
        var query = SqlQueryParser.standard().parse("select 1 limit all", PG_ANALYZE);
        var rule = LimitInjectionRewriteRule.of(
            new BuiltInRewriteSettings(10, 10, LimitExcessMode.DENY)
        );

        var ex = assertThrows(RewriteDenyException.class, () -> rule.apply(query, PG_ANALYZE));
        assertEquals(ReasonCode.DENY_MAX_ROWS, ex.reasonCode());
        assertTrue(ex.getMessage().contains("ALL"));
    }

    @Test
    void clamp_mode_clamps_select_composite_and_with_body_limits() {
        var settings = new BuiltInRewriteSettings(1000, 10, LimitExcessMode.CLAMP);
        var rule = LimitInjectionRewriteRule.of(settings);

        var select = SqlQueryParser.standard().parse("select 1 limit 99", PG_ANALYZE);
        var composite = SqlQueryParser.standard().parse("select 1 union all select 2 limit all", PG_ANALYZE);
        var withQuery = SqlQueryParser.standard().parse("with x as (select 1) select 1 limit 77", PG_ANALYZE);

        var selectResult = rule.apply(select, PG_ANALYZE);
        var compositeResult = rule.apply(composite, PG_ANALYZE);
        var withResult = rule.apply(withQuery, PG_ANALYZE);

        assertTrue(selectResult.rewritten());
        assertTrue(compositeResult.rewritten());
        assertTrue(withResult.rewritten());
        assertEquals(ReasonCode.REWRITE_LIMIT, selectResult.primaryReasonCode());
        assertTrue(SqlQueryRenderer.standard().render(selectResult.query(), PG_ANALYZE).sql().toLowerCase().contains("limit 10"));
        assertTrue(SqlQueryRenderer.standard().render(compositeResult.query(), PG_ANALYZE).sql().toLowerCase().contains("limit 10"));
        assertTrue(SqlQueryRenderer.standard().render(withResult.query(), PG_ANALYZE).sql().toLowerCase().contains("limit 10"));
    }

    @Test
    void max_limit_policy_keeps_queries_unchanged_when_within_bound_or_no_limit_after_injection() {
        var settings = new BuiltInRewriteSettings(5, 10, LimitExcessMode.DENY);
        var rule = LimitInjectionRewriteRule.of(settings);

        var alreadyWithinLimit = SqlQueryParser.standard().parse("select 1 limit 5", PG_ANALYZE);
        var offsetOnly = SqlQueryParser.standard().parse("select 1 offset 3", PG_ANALYZE);

        var withinResult = rule.apply(alreadyWithinLimit, PG_ANALYZE);
        var offsetResult = rule.apply(offsetOnly, PG_ANALYZE);

        assertFalse(withinResult.rewritten());
        assertTrue(offsetResult.rewritten());
        assertTrue(SqlQueryRenderer.standard().render(offsetResult.query(), PG_ANALYZE).sql().toLowerCase().contains("limit 5"));
    }
}

