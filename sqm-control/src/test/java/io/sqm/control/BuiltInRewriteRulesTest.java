package io.sqm.control;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.control.rewrite.BuiltInRewriteRules;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.select;
import static io.sqm.dsl.Dsl.unary;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuiltInRewriteRulesTest {
    private static final CatalogSchema SCHEMA = CatalogSchema.of(
        CatalogTable.of("public", "users", CatalogColumn.of("id", CatalogType.LONG))
    );

    private static final ExecutionContext POSTGRES_ANALYZE = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);

    @Test
    void all_available_returns_non_schema_rules_in_definition_order() {
        var rules = BuiltInRewriteRules.allAvailable(BuiltInRewriteSettings.defaults());

        assertEquals(3, rules.size());

        var query = SqlQueryParser.standard().parse("select 1", POSTGRES_ANALYZE);
        var result = SqlQueryRewriter.chain(rules.toArray(QueryRewriteRule[]::new)).rewrite(query, POSTGRES_ANALYZE);

        assertTrue(result.rewritten());
        assertEquals(ReasonCode.REWRITE_LIMIT, result.primaryReasonCode());
        assertEquals(List.of("limit-injection"), result.appliedRuleIds());
    }

    @Test
    void selected_non_schema_rules_support_explicit_selection_and_ordering() {
        var settings = BuiltInRewriteSettings.builder().defaultLimitInjectionValue(42).build();
        var rules = BuiltInRewriteRules.selected(
            settings,
            Set.of(BuiltInRewriteRule.CANONICALIZATION, BuiltInRewriteRule.LIMIT_INJECTION)
        );

        var query = SqlQueryParser.standard().parse("select 1 + 0", POSTGRES_ANALYZE);
        var result = SqlQueryRewriter.chain(rules.toArray(QueryRewriteRule[]::new)).rewrite(query, POSTGRES_ANALYZE);

        assertTrue(result.rewritten());
        assertEquals(ReasonCode.REWRITE_LIMIT, result.primaryReasonCode());
        assertEquals(List.of("limit-injection", "canonicalization"), result.appliedRuleIds());
        String rendered = SqlQueryRenderer.standard().render(result.query(), POSTGRES_ANALYZE).sql().toLowerCase();
        assertTrue(rendered.contains("limit 42"));
    }

    @Test
    void all_available_schema_rules_include_qualification_rules() {
        var rules = BuiltInRewriteRules.allAvailable(SCHEMA, BuiltInRewriteSettings.defaults());

        assertEquals(5, rules.size());

        var query = SqlQueryParser.standard().parse("select id from users limit 5", POSTGRES_ANALYZE);
        var result = SqlQueryRewriter.chain(rules.toArray(QueryRewriteRule[]::new)).rewrite(query, POSTGRES_ANALYZE);

        assertTrue(result.rewritten());
        assertEquals(ReasonCode.REWRITE_QUALIFICATION, result.primaryReasonCode());
        assertEquals(List.of("schema-qualification", "column-qualification"), result.appliedRuleIds());
    }

    @Test
    void selected_schema_rules_honor_qualification_failure_policy() {
        var unresolvedSchema = CatalogSchema.of(
            CatalogTable.of("public", "other", CatalogColumn.of("id", CatalogType.LONG))
        );
        var query = SqlQueryParser.standard().parse("select id from users limit 5", POSTGRES_ANALYZE);

        var strictRules = BuiltInRewriteRules.selected(
            unresolvedSchema,
            BuiltInRewriteSettings.builder().qualificationFailureMode(QualificationFailureMode.DENY).build(),
            Set.of(BuiltInRewriteRule.SCHEMA_QUALIFICATION)
        );
        var skipRules = BuiltInRewriteRules.selected(
            unresolvedSchema,
            BuiltInRewriteSettings.builder().qualificationFailureMode(QualificationFailureMode.SKIP).build(),
            Set.of(BuiltInRewriteRule.SCHEMA_QUALIFICATION)
        );

        assertThrows(
            RewriteDenyException.class,
            () -> SqlQueryRewriter.chain(strictRules.toArray(QueryRewriteRule[]::new)).rewrite(query, POSTGRES_ANALYZE)
        );

        var skipResult = SqlQueryRewriter.chain(skipRules.toArray(QueryRewriteRule[]::new)).rewrite(query, POSTGRES_ANALYZE);
        assertFalse(skipResult.rewritten());
    }

    @Test
    void validates_arguments_and_unsupported_rule_sets() {
        var settings = BuiltInRewriteSettings.defaults();

        assertThrows(NullPointerException.class, () -> BuiltInRewriteRules.allAvailable(null));
        assertThrows(NullPointerException.class, () -> BuiltInRewriteRules.selected(null, Set.of()));
        assertThrows(NullPointerException.class, () -> BuiltInRewriteRules.selected(settings, null));
        assertThrows(NullPointerException.class, () -> BuiltInRewriteRules.allAvailable(null, settings));
        assertThrows(NullPointerException.class, () -> BuiltInRewriteRules.selected(null, settings, Set.of()));
        assertThrows(NullPointerException.class, () -> BuiltInRewriteRules.selected(SCHEMA, null, Set.of()));
        assertThrows(NullPointerException.class, () -> BuiltInRewriteRules.selected(SCHEMA, settings, null));

        assertThrows(
            IllegalArgumentException.class,
            () -> BuiltInRewriteRules.selected(settings, Set.of(BuiltInRewriteRule.SCHEMA_QUALIFICATION))
        );
    }

    @Test
    void canonicalization_rule_from_source_can_simplify_boolean_predicates() {
        var query = select(lit(1))
            .limit(lit(5))
            .where(unary(lit(true)).and(col("active").eq(true)).or(unary(lit(false))))
            .build();

        var rules = BuiltInRewriteRules.selected(
            BuiltInRewriteSettings.defaults(),
            Set.of(BuiltInRewriteRule.CANONICALIZATION)
        );
        var result = SqlQueryRewriter.chain(rules.toArray(QueryRewriteRule[]::new)).rewrite(query, POSTGRES_ANALYZE);

        assertTrue(result.rewritten());
        assertEquals(ReasonCode.REWRITE_CANONICALIZATION, result.primaryReasonCode());
    }

    @Test
    void selected_rules_are_returned_in_definition_order_for_non_schema_and_schema_paths() {
        var unordered = Set.of(
            BuiltInRewriteRule.CANONICALIZATION,
            BuiltInRewriteRule.LIMIT_INJECTION,
            BuiltInRewriteRule.SCHEMA_QUALIFICATION,
            BuiltInRewriteRule.COLUMN_QUALIFICATION
        );

        var nonSchemaRules = BuiltInRewriteRules.selected(
            BuiltInRewriteSettings.builder().defaultLimitInjectionValue(12).build(),
            Set.of(BuiltInRewriteRule.CANONICALIZATION, BuiltInRewriteRule.LIMIT_INJECTION)
        );
        var nonSchemaQuery = SqlQueryParser.standard().parse("select 1 + 0", POSTGRES_ANALYZE);
        var nonSchemaResult = SqlQueryRewriter.chain(nonSchemaRules.toArray(QueryRewriteRule[]::new))
            .rewrite(nonSchemaQuery, POSTGRES_ANALYZE);
        assertEquals(List.of("limit-injection", "canonicalization"), nonSchemaResult.appliedRuleIds());

        var schemaRules = BuiltInRewriteRules.selected(
            SCHEMA,
            BuiltInRewriteSettings.builder().defaultLimitInjectionValue(12).build(),
            unordered
        );
        assertEquals(
            List.of(
                "LimitInjectionRewriteRule",
                "SchemaQualificationRewriteRule",
                "ColumnQualificationRewriteRule",
                "CanonicalizationRewriteRule"
            ),
            schemaRules.stream().map(rule -> rule.getClass().getSimpleName()).toList()
        );
        var schemaQuery = SqlQueryParser.standard().parse("select id from users", POSTGRES_ANALYZE);
        var schemaResult = SqlQueryRewriter.chain(schemaRules.toArray(QueryRewriteRule[]::new))
            .rewrite(schemaQuery, POSTGRES_ANALYZE);
        assertEquals(List.of("limit-injection", "schema-qualification", "column-qualification"),
            schemaResult.appliedRuleIds());
    }
}
