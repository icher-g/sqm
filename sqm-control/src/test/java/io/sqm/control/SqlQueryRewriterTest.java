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
        Query query = Query.select(Expression.literal(1));

        QueryRewriteResult result = SqlQueryRewriter.noop().rewrite(query, ANALYZE);

        assertSame(query, result.query());
        assertFalse(result.rewritten());
        assertEquals(List.of(), result.appliedRuleIds());
        assertEquals(ReasonCode.NONE, result.primaryReasonCode());
    }

    @Test
    void chain_applies_rules_in_order_and_accumulates_rule_ids() {
        Query input = Query.select(Expression.literal(1));
        Query intermediate = Query.select(Expression.literal(2));
        Query output = Query.select(Expression.literal(3));
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
        Query input = Query.select(Expression.literal(1));
        Query passThrough = Query.select(Expression.literal(2));

        QueryRewriteRule rule = (query, context) -> QueryRewriteResult.unchanged(passThrough);

        QueryRewriteResult result = SqlQueryRewriter.chain(rule).rewrite(input, ANALYZE);

        assertSame(passThrough, result.query());
        assertFalse(result.rewritten());
        assertEquals(List.of(), result.appliedRuleIds());
        assertEquals(ReasonCode.NONE, result.primaryReasonCode());
    }

    @Test
    void chain_validates_configuration_and_runtime_arguments() {
        Query query = Query.select(Expression.literal(1));
        QueryRewriteRule nullResultRule = (q, c) -> null;
        SqlQueryRewriter rewriter = SqlQueryRewriter.chain(nullResultRule);

        assertThrows(NullPointerException.class, () -> SqlQueryRewriter.chain((List<QueryRewriteRule>) null));
        //noinspection DataFlowIssue
        assertThrows(NullPointerException.class, () -> SqlQueryRewriter.chain(List.of((QueryRewriteRule) null)));
        assertThrows(NullPointerException.class, () -> SqlQueryRewriter.chain((QueryRewriteRule[]) null));
        assertThrows(NullPointerException.class, () -> rewriter.rewrite(null, ANALYZE));
        assertThrows(NullPointerException.class, () -> rewriter.rewrite(query, null));
        assertThrows(NullPointerException.class, () -> rewriter.rewrite(query, ANALYZE));
    }

    @Test
    void built_in_factory_methods_delegate_and_validate_arguments() {
        Query query = Query.select(Expression.literal(1));

        QueryRewriteResult allBuiltIn = SqlQueryRewriter.allBuiltIn().rewrite(query, ANALYZE);
        QueryRewriteResult noneSelected = SqlQueryRewriter.builtIn(Set.of()).rewrite(query, ANALYZE);

        assertTrue(allBuiltIn.rewritten());
        assertEquals(ReasonCode.REWRITE_LIMIT, allBuiltIn.primaryReasonCode());
        assertSame(query, noneSelected.query());
        assertFalse(noneSelected.rewritten());
        assertThrows(NullPointerException.class, () -> SqlQueryRewriter.builtIn((BuiltInRewriteRule[]) null));
        assertThrows(NullPointerException.class, () -> SqlQueryRewriter.builtIn((Set<BuiltInRewriteRule>) null));
        assertThrows(NullPointerException.class, () -> SqlQueryRewriter.builtIn(new BuiltInRewriteSettings(5), (Set<BuiltInRewriteRule>) null));
        assertThrows(IllegalArgumentException.class, () -> SqlQueryRewriter.builtIn(BuiltInRewriteRule.IDENTIFIER_NORMALIZATION));
    }

    @Test
    void schema_and_settings_built_in_factories_delegate() {
        QueryRewriteResult schemaAll = SqlQueryRewriter.allBuiltIn(SCHEMA)
            .rewrite(SqlQueryParser.standard().parse("select id from users", ANALYZE), ANALYZE);
        QueryRewriteResult configured = SqlQueryRewriter.builtIn(
            SCHEMA,
            new BuiltInRewriteSettings(23),
            BuiltInRewriteRule.LIMIT_INJECTION,
            BuiltInRewriteRule.SCHEMA_QUALIFICATION
        ).rewrite(SqlQueryParser.standard().parse("select id from users", ANALYZE), ANALYZE);

        assertTrue(schemaAll.rewritten());
        assertTrue(configured.rewritten());
        assertEquals(List.of("limit-injection", "schema-qualification"), configured.appliedRuleIds());
        String rendered = SqlQueryRenderer.postgresql().render(configured.query(), ANALYZE).toLowerCase();
        assertTrue(rendered.contains("public.users"));
        assertTrue(rendered.contains("limit 23"));
        assertThrows(NullPointerException.class, () -> SqlQueryRewriter.allBuiltIn((CatalogSchema) null));
        assertThrows(NullPointerException.class, () -> SqlQueryRewriter.allBuiltIn(SCHEMA, null));
        assertThrows(NullPointerException.class, () -> SqlQueryRewriter.builtIn((BuiltInRewriteSettings) null, BuiltInRewriteRule.LIMIT_INJECTION));
        assertThrows(NullPointerException.class, () -> SqlQueryRewriter.builtIn(SCHEMA, (BuiltInRewriteRule[]) null));
        assertThrows(NullPointerException.class, () -> SqlQueryRewriter.builtIn(SCHEMA, new BuiltInRewriteSettings(5), (Set<BuiltInRewriteRule>) null));
    }

    @Test
    void settings_and_set_based_built_in_factories_delegate_and_empty_chain_is_noop() {
        Query query = Query.select(Expression.literal(1));

        var configured = SqlQueryRewriter.builtIn(
            new BuiltInRewriteSettings(11),
            Set.of(BuiltInRewriteRule.LIMIT_INJECTION)
        ).rewrite(query, ANALYZE);
        var schemaConfigured = SqlQueryRewriter.builtIn(
            SCHEMA,
            new BuiltInRewriteSettings(13),
            Set.of(BuiltInRewriteRule.LIMIT_INJECTION, BuiltInRewriteRule.SCHEMA_QUALIFICATION)
        ).rewrite(SqlQueryParser.standard().parse("select id from users", ANALYZE), ANALYZE);
        var emptyVarargsChain = SqlQueryRewriter.chain().rewrite(query, ANALYZE);
        var emptyListChain = SqlQueryRewriter.chain(List.of()).rewrite(query, ANALYZE);

        assertTrue(configured.rewritten());
        assertTrue(SqlQueryRenderer.postgresql().render(configured.query(), ANALYZE).toLowerCase().contains("limit 11"));
        assertTrue(schemaConfigured.rewritten());
        assertFalse(emptyVarargsChain.rewritten());
        assertFalse(emptyListChain.rewritten());
    }
}
