package io.sqm.control;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.core.Expression;
import io.sqm.core.Query;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlQueryRewriterTest {

    private static final ExecutionContext ANALYZE = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);
    private static final CatalogSchema SCHEMA = CatalogSchema.of(
        CatalogTable.of("public", "users", CatalogColumn.of("id", CatalogType.LONG))
    );

    @Test
    void noop_returns_unchanged_result() {
        Query query = Query.select(Expression.literal(1)).build();

        QueryRewriteResult result = SqlQueryRewriter.noop().rewrite(query, ANALYZE);

        assertSame(query, result.query());
        assertFalse(result.rewritten());
        assertEquals(List.of(), result.appliedRuleIds());
        assertEquals(ReasonCode.NONE, result.primaryReasonCode());
    }

    @Test
    void chain_applies_rules_in_order_and_accumulates_rule_ids() {
        Query input = Query.select(Expression.literal(1)).build();
        Query intermediate = Query.select(Expression.literal(2)).build();
        Query output = Query.select(Expression.literal(3)).build();
        AtomicReference<Query> secondRuleInput = new AtomicReference<>();

        QueryRewriteRule first = new QueryRewriteRule() {
            @Override
            public String id() {
                return "first";
            }

            @Override
            public QueryRewriteResult apply(Query query, ExecutionContext context) {
                assertSame(input, query);
                return QueryRewriteResult.rewritten(intermediate, id(), ReasonCode.REWRITE_LIMIT);
            }
        };

        QueryRewriteRule second = new QueryRewriteRule() {
            @Override
            public String id() {
                return "second";
            }

            @Override
            public QueryRewriteResult apply(Query query, ExecutionContext context) {
                secondRuleInput.set(query);
                return QueryRewriteResult.rewritten(output, id(), ReasonCode.REWRITE_QUALIFICATION);
            }
        };

        QueryRewriteResult result = SqlQueryRewriter.chain(first, second).rewrite(input, ANALYZE);

        assertSame(intermediate, secondRuleInput.get());
        assertSame(output, result.query());
        assertTrue(result.rewritten());
        assertEquals(List.of("first", "second"), result.appliedRuleIds());
        assertEquals(ReasonCode.REWRITE_LIMIT, result.primaryReasonCode());
    }

    @Test
    void chain_preserves_last_query_when_no_rule_rewrites() {
        Query input = Query.select(Expression.literal(1)).build();
        Query passThrough = Query.select(Expression.literal(2)).build();

        QueryRewriteRule rule = (query, context) -> QueryRewriteResult.unchanged(passThrough);

        QueryRewriteResult result = SqlQueryRewriter.chain(rule).rewrite(input, ANALYZE);

        assertSame(passThrough, result.query());
        assertFalse(result.rewritten());
        assertEquals(List.of(), result.appliedRuleIds());
        assertEquals(ReasonCode.NONE, result.primaryReasonCode());
    }

    @Test
    void chain_validates_configuration_and_runtime_arguments() {
        Query query = Query.select(Expression.literal(1)).build();
        QueryRewriteRule nullResultRule = (q, c) -> null;
        SqlQueryRewriter rewriter = SqlQueryRewriter.chain(nullResultRule);

        assertThrows(NullPointerException.class, () -> SqlQueryRewriter.chain((QueryRewriteRule) null));
        assertThrows(NullPointerException.class, () -> SqlQueryRewriter.chain((QueryRewriteRule[]) null));
        assertThrows(NullPointerException.class, () -> rewriter.rewrite(null, ANALYZE));
        assertThrows(NullPointerException.class, () -> rewriter.rewrite(query, null));
        assertThrows(NullPointerException.class, () -> rewriter.rewrite(query, ANALYZE));
    }

    @Test
    void builder_builds_and_validates_non_schema_rewriter() {
        Query query = Query.select(Expression.literal(1)).build();

        QueryRewriteResult allBuiltIn = SqlQueryRewriter.builder().build().rewrite(query, ANALYZE);
        QueryRewriteResult noneSelected = SqlQueryRewriter.builder().rules(Set.of()).build().rewrite(query, ANALYZE);

        assertTrue(allBuiltIn.rewritten());
        assertEquals(ReasonCode.REWRITE_LIMIT, allBuiltIn.primaryReasonCode());
        assertTrue(noneSelected.rewritten());
        assertEquals(ReasonCode.REWRITE_LIMIT, noneSelected.primaryReasonCode());
        assertThrows(NullPointerException.class, () -> SqlQueryRewriter.builder().rules((BuiltInRewriteRule[]) null));
        assertThrows(NullPointerException.class, () -> SqlQueryRewriter.builder().rules((Set<BuiltInRewriteRule>) null));
        assertThrows(NullPointerException.class, () -> SqlQueryRewriter.builder().settings(null));
    }

    @Test
    void builder_builds_schema_and_settings_rewriter() {
        QueryRewriteResult schemaAll = SqlQueryRewriter.builder()
            .schema(SCHEMA)
            .build()
            .rewrite(SqlQueryParser.standard().parse("select id from users", ANALYZE), ANALYZE);
        QueryRewriteResult configured = SqlQueryRewriter.builder()
            .schema(SCHEMA)
            .settings(BuiltInRewriteSettings.builder().defaultLimitInjectionValue(23).build())
            .rules(BuiltInRewriteRule.LIMIT_INJECTION, BuiltInRewriteRule.SCHEMA_QUALIFICATION)
            .build()
            .rewrite(SqlQueryParser.standard().parse("select id from users", ANALYZE), ANALYZE);

        assertTrue(schemaAll.rewritten());
        assertTrue(configured.rewritten());
        assertEquals(List.of("limit-injection", "schema-qualification"), configured.appliedRuleIds());
        String rendered = SqlQueryRenderer.standard().render(configured.query(), ANALYZE).sql().toLowerCase();
        assertTrue(rendered.contains("public.users"));
        assertTrue(rendered.contains("limit 23"));
        assertThrows(NullPointerException.class, () -> SqlQueryRewriter.builder().schema(null));
        assertThrows(NullPointerException.class, () -> SqlQueryRewriter.builder().settings(null));
        assertThrows(NullPointerException.class, () -> SqlQueryRewriter.builder().schema(SCHEMA).rules((BuiltInRewriteRule[]) null));
        assertThrows(NullPointerException.class, () -> SqlQueryRewriter.builder().schema(SCHEMA).rules((Set<BuiltInRewriteRule>) null));
    }

    @Test
    void builder_rules_set_and_empty_chain_is_noop() {
        Query query = Query.select(Expression.literal(1)).build();

        var configured = SqlQueryRewriter.builder()
            .settings(BuiltInRewriteSettings.builder().defaultLimitInjectionValue(11).build())
            .rules(Set.of(BuiltInRewriteRule.LIMIT_INJECTION))
            .build()
            .rewrite(query, ANALYZE);
        var schemaConfigured = SqlQueryRewriter.builder()
            .schema(SCHEMA)
            .settings(BuiltInRewriteSettings.builder().defaultLimitInjectionValue(13).build())
            .rules(Set.of(BuiltInRewriteRule.LIMIT_INJECTION, BuiltInRewriteRule.SCHEMA_QUALIFICATION))
            .build()
            .rewrite(SqlQueryParser.standard().parse("select id from users", ANALYZE), ANALYZE);
        var emptyVarargsChain = SqlQueryRewriter.chain().rewrite(query, ANALYZE);

        assertTrue(configured.rewritten());
        assertTrue(SqlQueryRenderer.standard().render(configured.query(), ANALYZE).sql().toLowerCase().contains("limit 11"));
        assertTrue(schemaConfigured.rewritten());
        assertFalse(emptyVarargsChain.rewritten());
    }

    @Test
    void built_in_identifier_normalization_rewrites_unquoted_names() {
        var query = SqlQueryParser.standard().parse("select U.ID from Public.Users as U limit 5", ANALYZE);

        var result = SqlQueryRewriter.builder()
            .rules(BuiltInRewriteRule.IDENTIFIER_NORMALIZATION)
            .build()
            .rewrite(query, ANALYZE);

        assertTrue(result.rewritten());
        assertEquals(ReasonCode.REWRITE_IDENTIFIER_NORMALIZATION, result.primaryReasonCode());
        assertEquals(List.of("identifier-normalization"), result.appliedRuleIds());
    }
}
