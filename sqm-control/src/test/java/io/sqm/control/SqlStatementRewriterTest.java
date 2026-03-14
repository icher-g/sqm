package io.sqm.control;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.control.decision.ReasonCode;
import io.sqm.control.execution.ExecutionContext;
import io.sqm.control.execution.ExecutionMode;
import io.sqm.control.pipeline.SqlStatementParser;
import io.sqm.control.pipeline.SqlStatementRenderer;
import io.sqm.control.pipeline.SqlStatementRewriter;
import io.sqm.control.pipeline.StatementRewriteResult;
import io.sqm.control.pipeline.StatementRewriteRule;
import io.sqm.control.rewrite.BuiltInRewriteRule;
import io.sqm.control.rewrite.BuiltInRewriteSettings;
import io.sqm.core.DeleteStatement;
import io.sqm.core.Expression;
import io.sqm.core.Query;
import io.sqm.core.Statement;
import io.sqm.core.UpdateStatement;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.tbl;
import static io.sqm.dsl.Dsl.update;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlStatementRewriterTest {

    private static final ExecutionContext ANALYZE = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);
    private static final CatalogSchema SCHEMA = CatalogSchema.of(
        CatalogTable.of("public", "users", CatalogColumn.of("id", CatalogType.LONG), CatalogColumn.of("name", CatalogType.STRING))
    );

    private static Query parseQuery(String sql) {
        return (Query) SqlStatementParser.standard().parse(sql, ANALYZE);
    }

    @Test
    void noop_returns_unchanged_result() {
        Statement statement = Query.select(Expression.literal(1)).build();

        StatementRewriteResult result = SqlStatementRewriter.noop().rewrite(statement, ANALYZE);

        assertSame(statement, result.statement());
        assertFalse(result.rewritten());
        assertEquals(List.of(), result.appliedRuleIds());
        assertEquals(ReasonCode.NONE, result.primaryReasonCode());
    }

    @Test
    void chain_applies_rules_in_order_and_accumulates_rule_ids() {
        Statement input = Query.select(Expression.literal(1)).build();
        Statement intermediate = Query.select(Expression.literal(2)).build();
        Statement output = Query.select(Expression.literal(3)).build();
        AtomicReference<Statement> secondRuleInput = new AtomicReference<>();

        StatementRewriteRule first = new StatementRewriteRule() {
            @Override
            public String id() {
                return "first";
            }

            @Override
            public StatementRewriteResult apply(Statement statement, ExecutionContext context) {
                assertSame(input, statement);
                return StatementRewriteResult.rewritten(intermediate, id(), ReasonCode.REWRITE_LIMIT);
            }
        };

        StatementRewriteRule second = new StatementRewriteRule() {
            @Override
            public String id() {
                return "second";
            }

            @Override
            public StatementRewriteResult apply(Statement statement, ExecutionContext context) {
                secondRuleInput.set(statement);
                return StatementRewriteResult.rewritten(output, id(), ReasonCode.REWRITE_QUALIFICATION);
            }
        };

        StatementRewriteResult result = SqlStatementRewriter.chain(first, second).rewrite(input, ANALYZE);

        assertSame(intermediate, secondRuleInput.get());
        assertSame(output, result.statement());
        assertTrue(result.rewritten());
        assertEquals(List.of("first", "second"), result.appliedRuleIds());
        assertEquals(ReasonCode.REWRITE_LIMIT, result.primaryReasonCode());
    }

    @Test
    void chain_preserves_last_statement_when_no_rule_rewrites() {
        Statement input = Query.select(Expression.literal(1)).build();
        Statement passThrough = DeleteStatement.of(tbl("users"), java.util.List.of(), java.util.List.of(), null, java.util.List.of(), java.util.List.of());

        StatementRewriteRule rule = (statement, context) -> StatementRewriteResult.unchanged(passThrough);

        StatementRewriteResult result = SqlStatementRewriter.chain(rule).rewrite(input, ANALYZE);

        assertSame(passThrough, result.statement());
        assertFalse(result.rewritten());
        assertEquals(List.of(), result.appliedRuleIds());
        assertEquals(ReasonCode.NONE, result.primaryReasonCode());
    }

    @Test
    void chain_validates_configuration_and_runtime_arguments() {
        Statement statement = Query.select(Expression.literal(1)).build();
        StatementRewriteRule nullResultRule = (q, c) -> null;
        SqlStatementRewriter rewriter = SqlStatementRewriter.chain(nullResultRule);

        assertThrows(NullPointerException.class, () -> SqlStatementRewriter.chain((StatementRewriteRule) null));
        assertThrows(NullPointerException.class, () -> SqlStatementRewriter.chain((StatementRewriteRule[]) null));
        assertThrows(NullPointerException.class, () -> rewriter.rewrite(null, ANALYZE));
        assertThrows(NullPointerException.class, () -> rewriter.rewrite(statement, null));
        assertThrows(NullPointerException.class, () -> rewriter.rewrite(statement, ANALYZE));
    }

    @Test
    void builder_builds_and_validates_non_schema_rewriter() {
        Statement statement = Query.select(Expression.literal(1)).build();

        StatementRewriteResult allBuiltIn = SqlStatementRewriter.builder().build().rewrite(statement, ANALYZE);
        StatementRewriteResult noneSelected = SqlStatementRewriter.builder().rules(Set.of()).build().rewrite(statement, ANALYZE);

        assertTrue(allBuiltIn.rewritten());
        assertEquals(ReasonCode.REWRITE_LIMIT, allBuiltIn.primaryReasonCode());
        assertTrue(noneSelected.rewritten());
        assertEquals(ReasonCode.REWRITE_LIMIT, noneSelected.primaryReasonCode());
        assertThrows(NullPointerException.class, () -> SqlStatementRewriter.builder().rules((BuiltInRewriteRule[]) null));
        assertThrows(NullPointerException.class, () -> SqlStatementRewriter.builder().rules((Set<BuiltInRewriteRule>) null));
        assertThrows(NullPointerException.class, () -> SqlStatementRewriter.builder().settings(null));
    }

    @Test
    void builder_builds_schema_and_settings_rewriter() {
        StatementRewriteResult schemaAll = SqlStatementRewriter.builder()
            .schema(SCHEMA)
            .build()
            .rewrite(parseQuery("select id from users"), ANALYZE);
        StatementRewriteResult configured = SqlStatementRewriter.builder()
            .schema(SCHEMA)
            .settings(BuiltInRewriteSettings.builder().defaultLimitInjectionValue(23).build())
            .rules(BuiltInRewriteRule.LIMIT_INJECTION, BuiltInRewriteRule.SCHEMA_QUALIFICATION)
            .build()
            .rewrite(parseQuery("select id from users"), ANALYZE);

        assertTrue(schemaAll.rewritten());
        assertTrue(configured.rewritten());
        assertEquals(List.of("limit-injection", "schema-qualification"), configured.appliedRuleIds());
        String rendered = SqlStatementRenderer.standard().render(configured.statement(), ANALYZE).sql().toLowerCase();
        assertTrue(rendered.contains("public.users"));
        assertTrue(rendered.contains("limit 23"));
        assertThrows(NullPointerException.class, () -> SqlStatementRewriter.builder().schema(null));
        assertThrows(NullPointerException.class, () -> SqlStatementRewriter.builder().settings(null));
        assertThrows(NullPointerException.class, () -> SqlStatementRewriter.builder().schema(SCHEMA).rules((BuiltInRewriteRule[]) null));
        assertThrows(NullPointerException.class, () -> SqlStatementRewriter.builder().schema(SCHEMA).rules((Set<BuiltInRewriteRule>) null));
    }

    @Test
    void builder_rules_set_and_empty_chain_is_noop() {
        Statement statement = Query.select(Expression.literal(1)).build();

        var configured = SqlStatementRewriter.builder()
            .settings(BuiltInRewriteSettings.builder().defaultLimitInjectionValue(11).build())
            .rules(Set.of(BuiltInRewriteRule.LIMIT_INJECTION))
            .build()
            .rewrite(statement, ANALYZE);
        var schemaConfigured = SqlStatementRewriter.builder()
            .schema(SCHEMA)
            .settings(BuiltInRewriteSettings.builder().defaultLimitInjectionValue(13).build())
            .rules(Set.of(BuiltInRewriteRule.LIMIT_INJECTION, BuiltInRewriteRule.SCHEMA_QUALIFICATION))
            .build()
            .rewrite(parseQuery("select id from users"), ANALYZE);
        var emptyVarargsChain = SqlStatementRewriter.chain().rewrite(statement, ANALYZE);

        assertTrue(configured.rewritten());
        assertTrue(SqlStatementRenderer.standard().render(configured.statement(), ANALYZE).sql().toLowerCase().contains("limit 11"));
        assertTrue(schemaConfigured.rewritten());
        assertFalse(emptyVarargsChain.rewritten());
    }

    @Test
    void built_in_identifier_normalization_rewrites_unquoted_names() {
        var query = parseQuery("select U.ID from Public.Users as U limit 5");

        var result = SqlStatementRewriter.builder()
            .rules(BuiltInRewriteRule.IDENTIFIER_NORMALIZATION)
            .build()
            .rewrite(query, ANALYZE);

        assertTrue(result.rewritten());
        assertEquals(ReasonCode.REWRITE_IDENTIFIER_NORMALIZATION, result.primaryReasonCode());
        assertEquals(List.of("identifier-normalization"), result.appliedRuleIds());
    }

    @Test
    void built_in_identifier_normalization_rewrites_update_statements() {
        UpdateStatement statement = update("Users")
            .set(io.sqm.core.Identifier.of("Name"), lit("Alice"))
            .where(col("ID").eq(lit(1)))
            .build();

        var result = SqlStatementRewriter.builder()
            .rules(BuiltInRewriteRule.IDENTIFIER_NORMALIZATION)
            .build()
            .rewrite(statement, ANALYZE);

        assertTrue(result.rewritten());
        String rendered = SqlStatementRenderer.standard().render(result.statement(), ANALYZE).sql().toLowerCase();
        assertTrue(rendered.contains("update users"));
        assertTrue(rendered.contains("set name"));
    }
}
