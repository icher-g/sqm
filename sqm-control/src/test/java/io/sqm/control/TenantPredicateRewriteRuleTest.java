package io.sqm.control;

import io.sqm.control.rewrite.TenantPredicateRewriteRule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.select;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantPredicateRewriteRuleTest {
    private static final ExecutionContext TENANT_ANALYZE = ExecutionContext.of(
        "postgresql",
        null,
        "tenant_a",
        ExecutionMode.ANALYZE
    );

    @Test
    void injects_tenant_predicate_into_top_level_select_tables() {
        var settings = BuiltInRewriteSettings.builder()
            .tenantTablePolicy("public.users", TenantRewriteTablePolicy.required("tenant_id"))
            .build();
        var rule = TenantPredicateRewriteRule.of(settings);

        var query = select(col("u", "id"))
            .from(tbl("public", "users").as("u"))
            .build();

        var result = rule.apply(query, TENANT_ANALYZE);
        var rendered = SqlQueryRenderer.standard().render(result.query(), TENANT_ANALYZE).sql().toLowerCase();

        assertTrue(result.rewritten());
        assertEquals("tenant-predicate", result.appliedRuleIds().getFirst());
        assertEquals(ReasonCode.REWRITE_TENANT_PREDICATE, result.primaryReasonCode());
        assertTrue(rendered.contains("u.tenant_id"));
        assertTrue(rendered.contains("'tenant_a'"));
    }

    @Test
    void injects_for_unqualified_table_when_single_mapping_exists() {
        var settings = BuiltInRewriteSettings.builder()
            .tenantTablePolicy("public.users", TenantRewriteTablePolicy.required("tenant_id"))
            .build();
        var rule = TenantPredicateRewriteRule.of(settings);

        var query = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .build();

        var result = rule.apply(query, TENANT_ANALYZE);
        var rendered = SqlQueryRenderer.standard().render(result.query(), TENANT_ANALYZE).sql().toLowerCase();

        assertTrue(result.rewritten());
        assertEquals(ReasonCode.REWRITE_TENANT_PREDICATE, result.primaryReasonCode());
        assertTrue(rendered.contains("u.tenant_id"));
        assertTrue(rendered.contains("'tenant_a'"));
    }

    @Test
    void appends_predicate_to_existing_where_clause() {
        var settings = BuiltInRewriteSettings.builder()
            .tenantTablePolicy("public.users", TenantRewriteTablePolicy.required("tenant_id"))
            .build();
        var rule = TenantPredicateRewriteRule.of(settings);

        var query = select(col("u", "id"))
            .from(tbl("public", "users").as("u"))
            .where(col("u", "active").eq(lit(true)))
            .build();

        var result = rule.apply(query, TENANT_ANALYZE);
        var rendered = SqlQueryRenderer.standard().render(result.query(), TENANT_ANALYZE).sql().toLowerCase();

        assertTrue(result.rewritten());
        assertTrue(rendered.contains("u.active = true"));
        assertTrue(rendered.contains("u.tenant_id"));
        assertTrue(rendered.contains(" and "));
    }

    @Test
    void required_mode_denies_when_tenant_context_is_missing() {
        var settings = BuiltInRewriteSettings.builder()
            .tenantTablePolicy("public.users", TenantRewriteTablePolicy.required("tenant_id"))
            .build();
        var rule = TenantPredicateRewriteRule.of(settings);
        var contextWithoutTenant = ExecutionContext.of("postgresql", null, null, ExecutionMode.ANALYZE);
        var query = select(lit(1))
            .from(tbl("public", "users").as("u"))
            .build();

        var ex = assertThrows(RewriteDenyException.class, () -> rule.apply(query, contextWithoutTenant));
        assertEquals(ReasonCode.DENY_TENANT_REQUIRED, ex.reasonCode());
    }

    @Test
    void optional_mode_skips_when_tenant_context_is_missing() {
        var settings = BuiltInRewriteSettings.builder()
            .tenantTablePolicy(
                "public.users",
                TenantRewriteTablePolicy.of("tenant_id", TenantRewriteTableMode.OPTIONAL)
            )
            .build();
        var rule = TenantPredicateRewriteRule.of(settings);
        var contextWithoutTenant = ExecutionContext.of("postgresql", null, null, ExecutionMode.ANALYZE);
        var query = select(lit(1))
            .from(tbl("public", "users").as("u"))
            .build();

        var result = rule.apply(query, contextWithoutTenant);
        assertFalse(result.rewritten());
    }

    @Test
    void fallback_mode_controls_missing_mapping_behavior() {
        var query = select(lit(1))
            .from(tbl("public", "users").as("u"))
            .build();

        var denyRule = TenantPredicateRewriteRule.of(BuiltInRewriteSettings.builder()
            .tenantTablePolicy("public.events", TenantRewriteTablePolicy.required("tenant_id"))
            .tenantFallbackMode(TenantRewriteFallbackMode.DENY)
            .build());
        var skipRule = TenantPredicateRewriteRule.of(BuiltInRewriteSettings.builder()
            .tenantTablePolicy("public.events", TenantRewriteTablePolicy.required("tenant_id"))
            .tenantFallbackMode(TenantRewriteFallbackMode.SKIP)
            .build());

        var deny = assertThrows(RewriteDenyException.class, () -> denyRule.apply(query, TENANT_ANALYZE));
        assertEquals(ReasonCode.DENY_TENANT_MAPPING_MISSING, deny.reasonCode());
        assertFalse(skipRule.apply(query, TENANT_ANALYZE).rewritten());
    }

    @Test
    void ambiguity_mode_controls_unqualified_table_mapping_behavior() {
        var query = select(lit(1))
            .from(tbl("users").as("u"))
            .build();

        var denyRule = TenantPredicateRewriteRule.of(BuiltInRewriteSettings.builder()
            .tenantTablePolicy("tenant_a.users", TenantRewriteTablePolicy.required("tenant_id"))
            .tenantTablePolicy("tenant_b.users", TenantRewriteTablePolicy.required("tenant_id"))
            .tenantAmbiguityMode(TenantRewriteAmbiguityMode.DENY)
            .build());
        var skipRule = TenantPredicateRewriteRule.of(BuiltInRewriteSettings.builder()
            .tenantTablePolicy("tenant_a.users", TenantRewriteTablePolicy.required("tenant_id"))
            .tenantTablePolicy("tenant_b.users", TenantRewriteTablePolicy.required("tenant_id"))
            .tenantAmbiguityMode(TenantRewriteAmbiguityMode.SKIP)
            .build());

        var deny = assertThrows(RewriteDenyException.class, () -> denyRule.apply(query, TENANT_ANALYZE));
        assertEquals(ReasonCode.DENY_TENANT_MAPPING_AMBIGUOUS, deny.reasonCode());
        assertFalse(skipRule.apply(query, TENANT_ANALYZE).rewritten());
    }

    @Test
    void skips_injection_when_equivalent_tenant_predicate_already_exists_in_and_conjunction() {
        var settings = BuiltInRewriteSettings.builder()
            .tenantTablePolicy("public.users", TenantRewriteTablePolicy.required("tenant_id"))
            .build();
        var rule = TenantPredicateRewriteRule.of(settings);

        var query = select(col("u", "id"))
            .from(tbl("public", "users").as("u"))
            .where(col("u", "tenant_id").eq(lit("tenant_a")).and(col("u", "active").eq(lit(true))))
            .build();

        var result = rule.apply(query, TENANT_ANALYZE);
        assertFalse(result.rewritten());
    }

    @Test
    void does_not_treat_or_branch_predicate_as_duplicate_constraint() {
        var settings = BuiltInRewriteSettings.builder()
            .tenantTablePolicy("public.users", TenantRewriteTablePolicy.required("tenant_id"))
            .build();
        var rule = TenantPredicateRewriteRule.of(settings);

        var query = select(col("u", "id"))
            .from(tbl("public", "users").as("u"))
            .where(col("u", "tenant_id").eq(lit("tenant_a")).or(col("u", "active").eq(lit(true))))
            .build();

        var result = rule.apply(query, TENANT_ANALYZE);
        var rendered = SqlQueryRenderer.standard().render(result.query(), TENANT_ANALYZE).sql().toLowerCase();

        assertTrue(result.rewritten());
        assertTrue(rendered.contains("u.tenant_id = 'tenant_a'"));
        assertTrue(rendered.contains(" and "));
    }

    @Test
    void bind_mode_emits_tenant_param_and_preserves_parameter_order() {
        var settings = BuiltInRewriteSettings.builder()
            .tenantTablePolicy("public.users", TenantRewriteTablePolicy.required("tenant_id"))
            .build();
        var rule = TenantPredicateRewriteRule.of(settings);
        var bindContext = TENANT_ANALYZE.withParameterizationMode(ParameterizationMode.BIND);

        var query = select(col("u", "id"))
            .from(tbl("public", "users").as("u"))
            .where(col("u", "id").eq(lit(7)))
            .build();

        var rewritten = rule.apply(query, bindContext);
        var rendered = SqlQueryRenderer.standard().render(rewritten.query(), bindContext);

        assertTrue(rewritten.rewritten());
        assertEquals(ReasonCode.REWRITE_TENANT_PREDICATE, rewritten.primaryReasonCode());
        assertTrue(rendered.sql().contains("?"));
        assertEquals(2, rendered.params().size());
        assertEquals(7L, ((Number) rendered.params().getFirst()).longValue());
        assertEquals("tenant_a", rendered.params().get(1));
    }

    @Test
    void off_mode_inlines_tenant_literal_and_emits_no_params() {
        var settings = BuiltInRewriteSettings.builder()
            .tenantTablePolicy("public.users", TenantRewriteTablePolicy.required("tenant_id"))
            .build();
        var rule = TenantPredicateRewriteRule.of(settings);

        var query = select(col("u", "id"))
            .from(tbl("public", "users").as("u"))
            .where(col("u", "name").eq(lit("alice")))
            .build();

        var rewritten = rule.apply(query, TENANT_ANALYZE);
        var rendered = SqlQueryRenderer.standard().render(rewritten.query(), TENANT_ANALYZE);

        assertTrue(rewritten.rewritten());
        assertEquals(List.of(), rendered.params());
        assertTrue(rendered.sql().toLowerCase().contains("'tenant_a'"));
    }

    @Test
    void returns_unchanged_when_no_tenant_policies_are_configured() {
        var rule = TenantPredicateRewriteRule.of(BuiltInRewriteSettings.defaults());
        var query = select(col("u", "id"))
            .from(tbl("public", "users").as("u"))
            .build();

        var result = rule.apply(query, TENANT_ANALYZE);
        assertFalse(result.rewritten());
    }

    @Test
    void skip_mode_never_injects_predicate() {
        var settings = BuiltInRewriteSettings.builder()
            .tenantTablePolicy("public.users", TenantRewriteTablePolicy.of("tenant_id", TenantRewriteTableMode.SKIP))
            .build();
        var rule = TenantPredicateRewriteRule.of(settings);
        var query = select(col("u", "id"))
            .from(tbl("public", "users").as("u"))
            .build();

        var result = rule.apply(query, TENANT_ANALYZE);
        assertFalse(result.rewritten());
    }

    @Test
    void preferred_schema_resolves_ambiguous_unqualified_table() {
        var settings = BuiltInRewriteSettings.builder()
            .qualificationDefaultSchema("tenant_b")
            .tenantTablePolicy("tenant_a.users", TenantRewriteTablePolicy.required("tenant_a_id"))
            .tenantTablePolicy("tenant_b.users", TenantRewriteTablePolicy.required("tenant_b_id"))
            .build();
        var rule = TenantPredicateRewriteRule.of(settings);

        var query = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .build();

        var result = rule.apply(query, TENANT_ANALYZE);
        var rendered = SqlQueryRenderer.standard().render(result.query(), TENANT_ANALYZE).sql().toLowerCase();

        assertTrue(result.rewritten());
        assertTrue(rendered.contains("tenant_b_id"));
        assertNotEquals(-1, rendered.indexOf("tenant_b_id"));
    }

    @Test
    void injects_when_table_has_no_alias_using_table_name_as_qualifier() {
        var settings = BuiltInRewriteSettings.builder()
            .tenantTablePolicy("public.users", TenantRewriteTablePolicy.required("tenant_id"))
            .build();
        var rule = TenantPredicateRewriteRule.of(settings);
        var query = select(col("users", "id"))
            .from(tbl("public", "users"))
            .build();

        var result = rule.apply(query, TENANT_ANALYZE);
        var rendered = SqlQueryRenderer.standard().render(result.query(), TENANT_ANALYZE).sql().toLowerCase();

        assertTrue(result.rewritten());
        assertTrue(rendered.contains("users.tenant_id"));
    }
}
