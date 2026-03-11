package io.sqm.control;

import io.sqm.control.audit.*;
import io.sqm.control.config.*;
import io.sqm.control.decision.*;
import io.sqm.control.execution.*;
import io.sqm.control.pipeline.*;
import io.sqm.control.rewrite.*;
import io.sqm.control.service.*;

import io.sqm.core.Query;
import io.sqm.control.rewrite.LimitInjectionRewriteRule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimitInjectionRewriteRuleTest {
    private static final ExecutionContext PG_ANALYZE = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);

    private static Query parseQuery(String sql) {
        return (Query) SqlStatementParser.standard().parse(sql, PG_ANALYZE);
    }

    @Test
    void id_and_null_arguments_are_validated() {
        var rule = LimitInjectionRewriteRule.of(10);
        var query = parseQuery("select 1");

        assertEquals("limit-injection", rule.id());
        assertThrows(NullPointerException.class, () -> rule.apply(null, PG_ANALYZE));
        assertThrows(NullPointerException.class, () -> rule.apply(query, null));
        assertThrows(NullPointerException.class, () -> LimitInjectionRewriteRule.of(null));
    }

    @Test
    void deny_mode_rejects_non_literal_limit_expression() {
        var query = parseQuery("select 1 limit (1 + 1)");
        var rule = LimitInjectionRewriteRule.of(
            BuiltInRewriteSettings.builder()
                .defaultLimitInjectionValue(10)
                .maxAllowedLimit(10)
                .limitExcessMode(LimitExcessMode.DENY)
                .build()
        );

        var ex = assertThrows(RewriteDenyException.class, () -> rule.apply(query, PG_ANALYZE));
        assertEquals(ReasonCode.DENY_MAX_ROWS, ex.reasonCode());
        assertTrue(ex.getMessage().contains("literal value"));
    }

    @Test
    void deny_mode_rejects_limit_all() {
        var query = parseQuery("select 1 limit all");
        var rule = LimitInjectionRewriteRule.of(
            BuiltInRewriteSettings.builder()
                .defaultLimitInjectionValue(10)
                .maxAllowedLimit(10)
                .limitExcessMode(LimitExcessMode.DENY)
                .build()
        );

        var ex = assertThrows(RewriteDenyException.class, () -> rule.apply(query, PG_ANALYZE));
        assertEquals(ReasonCode.DENY_MAX_ROWS, ex.reasonCode());
        assertTrue(ex.getMessage().contains("ALL"));
    }

    @Test
    void clamp_mode_clamps_select_composite_and_with_body_limits() {
        var settings = BuiltInRewriteSettings.builder()
            .defaultLimitInjectionValue(1000)
            .maxAllowedLimit(10)
            .limitExcessMode(LimitExcessMode.CLAMP)
            .build();
        var rule = LimitInjectionRewriteRule.of(settings);

        var select = parseQuery("select 1 limit 99");
        var composite = parseQuery("select 1 union all select 2 limit all");
        var withQuery = parseQuery("with x as (select 1) select 1 limit 77");

        var selectResult = rule.apply(select, PG_ANALYZE);
        var compositeResult = rule.apply(composite, PG_ANALYZE);
        var withResult = rule.apply(withQuery, PG_ANALYZE);

        assertTrue(selectResult.rewritten());
        assertTrue(compositeResult.rewritten());
        assertTrue(withResult.rewritten());
        assertEquals(ReasonCode.REWRITE_LIMIT, selectResult.primaryReasonCode());
        assertTrue(SqlStatementRenderer.standard().render(selectResult.statement(), PG_ANALYZE).sql().toLowerCase().contains("limit 10"));
        assertTrue(SqlStatementRenderer.standard().render(compositeResult.statement(), PG_ANALYZE).sql().toLowerCase().contains("limit 10"));
        assertTrue(SqlStatementRenderer.standard().render(withResult.statement(), PG_ANALYZE).sql().toLowerCase().contains("limit 10"));
    }

    @Test
    void max_limit_policy_keeps_queries_unchanged_when_within_bound_or_no_limit_after_injection() {
        var settings = BuiltInRewriteSettings.builder()
            .defaultLimitInjectionValue(5)
            .maxAllowedLimit(10)
            .limitExcessMode(LimitExcessMode.DENY)
            .build();
        var rule = LimitInjectionRewriteRule.of(settings);

        var alreadyWithinLimit = parseQuery("select 1 limit 5");
        var offsetOnly = parseQuery("select 1 offset 3");

        var withinResult = rule.apply(alreadyWithinLimit, PG_ANALYZE);
        var offsetResult = rule.apply(offsetOnly, PG_ANALYZE);

        assertFalse(withinResult.rewritten());
        assertTrue(offsetResult.rewritten());
        assertTrue(SqlStatementRenderer.standard().render(offsetResult.statement(), PG_ANALYZE).sql().toLowerCase().contains("limit 5"));
    }

    @Test
    void clamp_mode_caps_default_limit_injected_from_settings() {
        var rule = LimitInjectionRewriteRule.of(
            BuiltInRewriteSettings.builder()
                .defaultLimitInjectionValue(100)
                .maxAllowedLimit(7)
                .limitExcessMode(LimitExcessMode.CLAMP)
                .build()
        );

        var query = parseQuery("select 1");
        var result = rule.apply(query, PG_ANALYZE);
        var rendered = SqlStatementRenderer.standard().render(result.statement(), PG_ANALYZE).sql().toLowerCase();

        assertTrue(result.rewritten());
        assertTrue(rendered.contains("limit 7"));
    }
}



