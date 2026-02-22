package io.sqm.control;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.control.rewrite.BuiltInSqlRewriters;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BuiltInSqlRewritersTest {
    private static final CatalogSchema SCHEMA = CatalogSchema.of(
        CatalogTable.of("public", "users", CatalogColumn.of("id", CatalogType.LONG))
    );
    private static final ExecutionContext POSTGRES_ANALYZE = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);

    @Test
    void all_available_applies_limit_injection() {
        var result = BuiltInSqlRewriters.allAvailable()
            .rewrite(io.sqm.core.Query.select(io.sqm.core.Expression.literal(1)),
                ExecutionContext.of("ansi", ExecutionMode.ANALYZE));

        assertTrue(result.rewritten());
        assertEquals(ReasonCode.REWRITE_LIMIT, result.primaryReasonCode());
        assertEquals(java.util.List.of("limit-injection"), result.appliedRuleIds());
    }

    @Test
    void available_rules_exposes_supported_rules_and_is_immutable() {
        Set<BuiltInRewriteRule> available = BuiltInSqlRewriters.availableRules();

        assertEquals(Set.of(BuiltInRewriteRule.LIMIT_INJECTION, BuiltInRewriteRule.CANONICALIZATION), available);
        //noinspection DataFlowIssue
        assertThrows(UnsupportedOperationException.class, () -> available.add(BuiltInRewriteRule.IDENTIFIER_NORMALIZATION));
    }

    @Test
    void selecting_unavailable_built_in_rule_fails_fast() {
        assertThrows(IllegalArgumentException.class,
            () -> BuiltInSqlRewriters.of(EnumSet.of(BuiltInRewriteRule.IDENTIFIER_NORMALIZATION)));
    }

    @Test
    void factory_validates_nulls_and_supports_empty_selection() {
        var query = io.sqm.core.Query.select(io.sqm.core.Expression.literal(1));
        var context = ExecutionContext.of("ansi", ExecutionMode.ANALYZE);

        assertThrows(NullPointerException.class, () -> BuiltInSqlRewriters.of((BuiltInRewriteRule[]) null));
        assertThrows(NullPointerException.class, () -> BuiltInSqlRewriters.of((Set<BuiltInRewriteRule>) null));

        var resultFromVarargs = BuiltInSqlRewriters.of().rewrite(query, context);
        var resultFromSet = BuiltInSqlRewriters.of(Set.of()).rewrite(query, context);
        assertFalse(resultFromVarargs.rewritten());
        assertFalse(resultFromSet.rewritten());
    }

    @Test
    void selecting_supported_limit_injection_rewrites_query() {
        var result = BuiltInSqlRewriters.of(BuiltInRewriteRule.LIMIT_INJECTION)
            .rewrite(io.sqm.core.Query.select(io.sqm.core.Expression.literal(1)),
                ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));

        assertTrue(result.rewritten());
        assertEquals(ReasonCode.REWRITE_LIMIT, result.primaryReasonCode());
    }

    @Test
    void limit_injection_leaves_query_unchanged_when_limit_exists() {
        var query = SqlQueryParser.standard().parse("select 1 limit 5", POSTGRES_ANALYZE);

        var result = BuiltInSqlRewriters.of(BuiltInRewriteRule.LIMIT_INJECTION)
            .rewrite(query, POSTGRES_ANALYZE);

        assertFalse(result.rewritten());
        assertEquals(ReasonCode.NONE, result.primaryReasonCode());
    }

    @Test
    void configurable_limit_injection_uses_provided_default_limit() {
        var query = SqlQueryParser.standard().parse("select 1", POSTGRES_ANALYZE);

        var result = BuiltInSqlRewriters.of(new BuiltInRewriteSettings(42), BuiltInRewriteRule.LIMIT_INJECTION)
            .rewrite(query, POSTGRES_ANALYZE);

        assertTrue(result.rewritten());
        assertEquals(List.of("limit-injection"), result.appliedRuleIds());
        String rendered = SqlQueryRenderer.postgresql().render(result.query(), POSTGRES_ANALYZE);
        assertTrue(rendered.toLowerCase().contains("limit 42"));
    }

    @Test
    void limit_excess_clamp_mode_rewrites_existing_limit_to_max() {
        var query = SqlQueryParser.standard().parse("select 1 limit 99", POSTGRES_ANALYZE);

        var result = BuiltInSqlRewriters.of(
            new BuiltInRewriteSettings(1000, 10, BuiltInRewriteSettings.LimitExcessMode.CLAMP),
            BuiltInRewriteRule.LIMIT_INJECTION
        ).rewrite(query, POSTGRES_ANALYZE);

        assertTrue(result.rewritten());
        assertEquals(ReasonCode.REWRITE_LIMIT, result.primaryReasonCode());
        String rendered = SqlQueryRenderer.postgresql().render(result.query(), POSTGRES_ANALYZE).toLowerCase();
        assertTrue(rendered.contains("limit 10"));
    }

    @Test
    void schema_qualification_rewrites_unqualified_table_when_schema_provided() {
        var query = SqlQueryParser.standard().parse("select id from users", POSTGRES_ANALYZE);

        var result = BuiltInSqlRewriters.forSchema(SCHEMA, BuiltInRewriteRule.SCHEMA_QUALIFICATION)
            .rewrite(query, POSTGRES_ANALYZE);

        assertTrue(result.rewritten());
        assertEquals(ReasonCode.REWRITE_QUALIFICATION, result.primaryReasonCode());
        assertEquals(List.of("schema-qualification"), result.appliedRuleIds());
        String rendered = SqlQueryRenderer.postgresql().render(result.query(), POSTGRES_ANALYZE);
        assertTrue(rendered.toLowerCase().contains("from public.users"));
    }

    @Test
    void schema_aware_rewriter_applies_rules_in_enum_order() {
        var query = SqlQueryParser.standard().parse("select id from users", POSTGRES_ANALYZE);

        var result = BuiltInSqlRewriters.forSchema(
            SCHEMA,
            new BuiltInRewriteSettings(17),
            BuiltInRewriteRule.SCHEMA_QUALIFICATION,
            BuiltInRewriteRule.LIMIT_INJECTION
        ).rewrite(query, POSTGRES_ANALYZE);

        assertTrue(result.rewritten());
        assertEquals(ReasonCode.REWRITE_LIMIT, result.primaryReasonCode());
        assertEquals(List.of("limit-injection", "schema-qualification"), result.appliedRuleIds());
        String rendered = SqlQueryRenderer.postgresql().render(result.query(), POSTGRES_ANALYZE).toLowerCase();
        assertTrue(rendered.contains("from public.users"));
        assertTrue(rendered.contains("limit 17"));
    }

    @Test
    void schema_aware_factories_validate_arguments() {
        assertThrows(NullPointerException.class, () -> BuiltInSqlRewriters.allAvailable((CatalogSchema) null));
        assertThrows(NullPointerException.class,
            () -> BuiltInSqlRewriters.forSchema(null, BuiltInRewriteRule.SCHEMA_QUALIFICATION));
        assertThrows(NullPointerException.class,
            () -> BuiltInSqlRewriters.forSchema(SCHEMA, (BuiltInRewriteSettings) null, BuiltInRewriteRule.LIMIT_INJECTION));
        assertThrows(NullPointerException.class,
            () -> BuiltInSqlRewriters.of((BuiltInRewriteSettings) null, BuiltInRewriteRule.LIMIT_INJECTION));
        assertThrows(IllegalArgumentException.class,
            () -> BuiltInSqlRewriters.of(BuiltInRewriteRule.SCHEMA_QUALIFICATION));
        assertThrows(IllegalArgumentException.class,
            () -> new BuiltInRewriteSettings(100, 10, BuiltInRewriteSettings.LimitExcessMode.DENY));
    }

    @Test
    void canonicalization_simplifies_arithmetic_expression() {
        var query = SqlQueryParser.standard().parse("select 1 + 0", POSTGRES_ANALYZE);

        var result = BuiltInSqlRewriters.of(BuiltInRewriteRule.CANONICALIZATION)
            .rewrite(query, POSTGRES_ANALYZE);

        assertTrue(result.rewritten());
        assertEquals(ReasonCode.REWRITE_CANONICALIZATION, result.primaryReasonCode());
        assertEquals(List.of("canonicalization"), result.appliedRuleIds());
        String rendered = SqlQueryRenderer.postgresql().render(result.query(), POSTGRES_ANALYZE).toLowerCase();
        assertTrue(rendered.contains("select 1"));
        assertFalse(rendered.contains("+"));
    }
}
