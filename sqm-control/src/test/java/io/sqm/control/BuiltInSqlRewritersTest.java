package io.sqm.control;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.control.rewrite.BuiltInSqlRewriters;
import io.sqm.core.Expression;
import io.sqm.core.Query;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class BuiltInSqlRewritersTest {
    private static final CatalogSchema SCHEMA = CatalogSchema.of(
        CatalogTable.of("public", "users", CatalogColumn.of("id", CatalogType.LONG))
    );
    private static final ExecutionContext POSTGRES_ANALYZE = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);

    @Test
    void all_available_applies_limit_injection() {
        var result = BuiltInSqlRewriters.allAvailable()
            .rewrite(Query.select(Expression.literal(1)).build(),
                ExecutionContext.of("ansi", ExecutionMode.ANALYZE));

        assertTrue(result.rewritten());
        assertEquals(ReasonCode.REWRITE_LIMIT, result.primaryReasonCode());
        assertEquals(java.util.List.of("limit-injection"), result.appliedRuleIds());
    }

    @Test
    void available_rules_exposes_supported_rules_and_is_immutable() {
        Set<BuiltInRewriteRule> available = BuiltInSqlRewriters.availableRules();

        assertEquals(
            Set.of(
                BuiltInRewriteRule.LIMIT_INJECTION,
                BuiltInRewriteRule.IDENTIFIER_NORMALIZATION,
                BuiltInRewriteRule.CANONICALIZATION
            ),
            available
        );
        //noinspection DataFlowIssue
        assertThrows(UnsupportedOperationException.class, () -> available.add(BuiltInRewriteRule.LITERAL_PARAMETERIZATION));
    }

    @Test
    void selecting_unavailable_built_in_rule_fails_fast() {
        assertThrows(IllegalArgumentException.class,
            () -> BuiltInSqlRewriters.of(EnumSet.of(BuiltInRewriteRule.LITERAL_PARAMETERIZATION)));
    }

    @Test
    void factory_validates_nulls_and_supports_empty_selection() {
        var query = Query.select(Expression.literal(1)).build();
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
            .rewrite(Query.select(Expression.literal(1)).build(),
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
        String rendered = SqlQueryRenderer.standard().render(result.query(), POSTGRES_ANALYZE).sql();
        assertTrue(rendered.toLowerCase().contains("limit 42"));
    }

    @Test
    void limit_excess_clamp_mode_rewrites_existing_limit_to_max() {
        var query = SqlQueryParser.standard().parse("select 1 limit 99", POSTGRES_ANALYZE);

        var result = BuiltInSqlRewriters.of(
            new BuiltInRewriteSettings(1000, 10, LimitExcessMode.CLAMP),
            BuiltInRewriteRule.LIMIT_INJECTION
        ).rewrite(query, POSTGRES_ANALYZE);

        assertTrue(result.rewritten());
        assertEquals(ReasonCode.REWRITE_LIMIT, result.primaryReasonCode());
        String rendered = SqlQueryRenderer.standard().render(result.query(), POSTGRES_ANALYZE).sql().toLowerCase();
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
        String rendered = SqlQueryRenderer.standard().render(result.query(), POSTGRES_ANALYZE).sql();
        assertTrue(rendered.toLowerCase().contains("from public.users"));
    }

    @Test
    void schema_qualification_prefers_configured_default_schema_when_name_is_ambiguous() {
        var schema = CatalogSchema.of(
            CatalogTable.of("public", "users", CatalogColumn.of("id", CatalogType.LONG)),
            CatalogTable.of("tenant_a", "users", CatalogColumn.of("id", CatalogType.LONG))
        );
        var settings = new BuiltInRewriteSettings(
            1000,
            null,
            LimitExcessMode.DENY,
            "public",
            QualificationFailureMode.DENY
        );
        var query = SqlQueryParser.standard().parse("select id from users", POSTGRES_ANALYZE);

        var result = BuiltInSqlRewriters.forSchema(schema, settings, BuiltInRewriteRule.SCHEMA_QUALIFICATION)
            .rewrite(query, POSTGRES_ANALYZE);

        assertTrue(result.rewritten());
        assertEquals(ReasonCode.REWRITE_QUALIFICATION, result.primaryReasonCode());
        String rendered = SqlQueryRenderer.standard().render(result.query(), POSTGRES_ANALYZE).sql().toLowerCase();
        assertTrue(rendered.contains("from public.users"));
    }

    @Test
    void schema_qualification_uses_policy_for_unresolved_or_ambiguous_targets() {
        var unresolvedSchema = CatalogSchema.of(
            CatalogTable.of("public", "other", CatalogColumn.of("id", CatalogType.LONG))
        );
        var ambiguousSchema = CatalogSchema.of(
            CatalogTable.of("public", "users", CatalogColumn.of("id", CatalogType.LONG)),
            CatalogTable.of("tenant_a", "users", CatalogColumn.of("id", CatalogType.LONG))
        );
        var query = SqlQueryParser.standard().parse("select id from users", POSTGRES_ANALYZE);
        var strict = new BuiltInRewriteSettings(
            1000,
            null,
            LimitExcessMode.DENY,
            null,
            QualificationFailureMode.DENY
        );
        var skip = new BuiltInRewriteSettings(
            1000,
            null,
            LimitExcessMode.DENY,
            null,
            QualificationFailureMode.SKIP
        );

        assertThrows(
            RewriteDenyException.class,
            () -> BuiltInSqlRewriters.forSchema(unresolvedSchema, strict, BuiltInRewriteRule.SCHEMA_QUALIFICATION)
                .rewrite(query, POSTGRES_ANALYZE)
        );
        assertThrows(
            RewriteDenyException.class,
            () -> BuiltInSqlRewriters.forSchema(ambiguousSchema, strict, BuiltInRewriteRule.SCHEMA_QUALIFICATION)
                .rewrite(query, POSTGRES_ANALYZE)
        );
        assertFalse(BuiltInSqlRewriters.forSchema(unresolvedSchema, skip, BuiltInRewriteRule.SCHEMA_QUALIFICATION)
            .rewrite(query, POSTGRES_ANALYZE)
            .rewritten());
        assertFalse(BuiltInSqlRewriters.forSchema(ambiguousSchema, skip, BuiltInRewriteRule.SCHEMA_QUALIFICATION)
            .rewrite(query, POSTGRES_ANALYZE)
            .rewritten());
    }

    @Test
    void schema_aware_rewriter_applies_rules_in_enum_order() {
        var query = SqlQueryParser.standard().parse("select id from users", POSTGRES_ANALYZE);

        var result = BuiltInSqlRewriters.forSchema(
            SCHEMA,
            new BuiltInRewriteSettings(17),
            BuiltInRewriteRule.SCHEMA_QUALIFICATION,
            BuiltInRewriteRule.COLUMN_QUALIFICATION,
            BuiltInRewriteRule.LIMIT_INJECTION
        ).rewrite(query, POSTGRES_ANALYZE);

        assertTrue(result.rewritten());
        assertEquals(ReasonCode.REWRITE_LIMIT, result.primaryReasonCode());
        assertEquals(List.of("limit-injection", "schema-qualification", "column-qualification"), result.appliedRuleIds());
        String rendered = SqlQueryRenderer.standard().render(result.query(), POSTGRES_ANALYZE).sql().toLowerCase();
        assertTrue(rendered.contains("from public.users"));
        assertTrue(rendered.contains("select users.id") || rendered.contains("select public.users.id") || rendered.contains("select id"));
        assertTrue(rendered.contains("limit 17"));
    }

    @Test
    void column_qualification_rewrites_when_schema_rule_is_also_enabled() {
        var query = SqlQueryParser.standard().parse("select id from users u", POSTGRES_ANALYZE);

        var result = BuiltInSqlRewriters.forSchema(
            SCHEMA,
            BuiltInRewriteRule.SCHEMA_QUALIFICATION,
            BuiltInRewriteRule.COLUMN_QUALIFICATION
        ).rewrite(query, POSTGRES_ANALYZE);

        assertTrue(result.rewritten());
        assertEquals(List.of("schema-qualification", "column-qualification"), result.appliedRuleIds());
        String rendered = SqlQueryRenderer.standard().render(result.query(), POSTGRES_ANALYZE).sql().toLowerCase();
        assertTrue(rendered.contains("select u.id"));
        assertTrue(rendered.contains("from public.users as u"));
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
            () -> new BuiltInRewriteSettings(100, 10, LimitExcessMode.DENY));
    }

    @Test
    void canonicalization_simplifies_arithmetic_expression() {
        var query = SqlQueryParser.standard().parse("select 1 + 0", POSTGRES_ANALYZE);

        var result = BuiltInSqlRewriters.of(BuiltInRewriteRule.CANONICALIZATION)
            .rewrite(query, POSTGRES_ANALYZE);

        assertTrue(result.rewritten());
        assertEquals(ReasonCode.REWRITE_CANONICALIZATION, result.primaryReasonCode());
        assertEquals(List.of("canonicalization"), result.appliedRuleIds());
        String rendered = SqlQueryRenderer.standard().render(result.query(), POSTGRES_ANALYZE).sql().toLowerCase();
        assertTrue(rendered.contains("select 1"));
        assertFalse(rendered.contains("+"));
    }

    @Test
    void canonicalization_simplifies_boolean_predicates() {
        var query = select(lit(1))
            .where(unary(lit(true)).and(col("active").eq(true)).or(unary(lit(false))))
            .build();

        var result = BuiltInSqlRewriters.of(BuiltInRewriteRule.CANONICALIZATION)
            .rewrite(query, POSTGRES_ANALYZE);

        assertTrue(result.rewritten());
        assertEquals(ReasonCode.REWRITE_CANONICALIZATION, result.primaryReasonCode());
        String rendered = SqlQueryRenderer.standard().render(result.query(), POSTGRES_ANALYZE).sql();
        assertTrue(rendered.contains("WHERE active = TRUE"));
        assertFalse(rendered.contains("TRUE AND"));
        assertFalse(rendered.contains("OR FALSE"));
    }

    @Test
    void identifier_normalization_rewrites_unquoted_identifiers_only() {
        var query = SqlQueryParser.standard().parse("select U.ID as CNT from Public.Users as U", POSTGRES_ANALYZE);

        var result = BuiltInSqlRewriters.of(BuiltInRewriteRule.IDENTIFIER_NORMALIZATION)
            .rewrite(query, POSTGRES_ANALYZE);

        assertTrue(result.rewritten());
        assertEquals(ReasonCode.REWRITE_IDENTIFIER_NORMALIZATION, result.primaryReasonCode());
        assertEquals(List.of("identifier-normalization"), result.appliedRuleIds());
        String rendered = SqlQueryRenderer.standard().render(result.query(), POSTGRES_ANALYZE).sql();
        assertTrue(rendered.toLowerCase().contains("from public.users as u"));
        assertTrue(rendered.toLowerCase().contains("select u.id as cnt"));
    }

    @Test
    void identifier_normalization_leaves_quoted_identifiers_unchanged() {
        var query = SqlQueryParser.standard().parse("select \"U\".\"ID\" from \"Public\".\"Users\" as \"U\"", POSTGRES_ANALYZE);

        var result = BuiltInSqlRewriters.of(BuiltInRewriteRule.IDENTIFIER_NORMALIZATION)
            .rewrite(query, POSTGRES_ANALYZE);

        assertFalse(result.rewritten());
        assertEquals(ReasonCode.NONE, result.primaryReasonCode());
    }

    @Test
    void identifier_normalization_case_mode_can_be_configured() {
        var query = SqlQueryParser.standard().parse("select u.id as cnt from public.users as u", POSTGRES_ANALYZE);
        var settings = new BuiltInRewriteSettings(
            1000,
            null,
            LimitExcessMode.DENY,
            null,
            QualificationFailureMode.DENY,
            io.sqm.core.transform.IdentifierNormalizationCaseMode.UPPER
        );

        var result = BuiltInSqlRewriters.of(settings, BuiltInRewriteRule.IDENTIFIER_NORMALIZATION)
            .rewrite(query, POSTGRES_ANALYZE);

        assertTrue(result.rewritten());
        assertEquals(ReasonCode.REWRITE_IDENTIFIER_NORMALIZATION, result.primaryReasonCode());
        String rendered = SqlQueryRenderer.standard().render(result.query(), POSTGRES_ANALYZE).sql();
        assertTrue(rendered.contains("SELECT U.ID AS CNT"));
        assertTrue(rendered.contains("FROM PUBLIC.USERS AS U"));
    }
}
