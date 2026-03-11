package io.sqm.control;

import io.sqm.control.audit.*;
import io.sqm.control.config.*;
import io.sqm.control.decision.*;
import io.sqm.control.execution.*;
import io.sqm.control.pipeline.*;
import io.sqm.control.rewrite.*;
import io.sqm.control.service.*;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.control.rewrite.ColumnQualificationRewriteRule;
import io.sqm.core.Query;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.insert;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.row;
import static org.junit.jupiter.api.Assertions.*;

class ColumnQualificationRewriteRuleTest {
    private static final ExecutionContext PG_ANALYZE = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);

    private static Query parseQuery(String sql) {
        return (Query) SqlStatementParser.standard().parse(sql, PG_ANALYZE);
    }

    @Test
    void qualifies_columns_using_visible_table_aliases_and_catalog_metadata() {
        var schema = CatalogSchema.of(CatalogTable.of("public", "users", CatalogColumn.of("id", CatalogType.LONG)));
        var settings = BuiltInRewriteSettings.defaults();
        var rule = ColumnQualificationRewriteRule.of(schema, settings);
        var query = parseQuery("select id from users u");

        var result = rule.apply(query, PG_ANALYZE);

        assertTrue(result.rewritten());
        assertEquals(ReasonCode.REWRITE_QUALIFICATION, result.primaryReasonCode());
        String rendered = SqlStatementRenderer.standard().render(result.statement(), PG_ANALYZE).sql().toLowerCase();
        assertTrue(rendered.contains("select u.id"));
    }

    @Test
    void strict_mode_denies_unresolved_or_ambiguous_columns_and_skip_mode_leaves_unchanged() {
        var unresolvedSchema = CatalogSchema.of(CatalogTable.of("public", "users", CatalogColumn.of("name", CatalogType.STRING)));
        var ambiguousSchema = CatalogSchema.of(
            CatalogTable.of("public", "users", CatalogColumn.of("id", CatalogType.LONG)),
            CatalogTable.of("public", "orders", CatalogColumn.of("id", CatalogType.LONG))
        );
        var strict = BuiltInRewriteSettings.defaults();
        var skip = BuiltInRewriteSettings.builder(strict)
            .qualificationFailureMode(QualificationFailureMode.SKIP)
            .build();
        var unresolvedQuery = parseQuery("select id from users u");
        var ambiguousQuery = parseQuery("select id from users u join orders o on u.id = o.id");

        var unresolvedError = assertThrows(
            RewriteDenyException.class,
            () -> ColumnQualificationRewriteRule.of(unresolvedSchema, strict).apply(unresolvedQuery, PG_ANALYZE)
        );
        assertEquals(ReasonCode.DENY_COLUMN, unresolvedError.reasonCode());

        var ambiguousError = assertThrows(
            RewriteDenyException.class,
            () -> ColumnQualificationRewriteRule.of(ambiguousSchema, strict).apply(ambiguousQuery, PG_ANALYZE)
        );
        assertEquals(ReasonCode.DENY_COLUMN, ambiguousError.reasonCode());

        assertFalse(ColumnQualificationRewriteRule.of(unresolvedSchema, skip).apply(unresolvedQuery, PG_ANALYZE).rewritten());
        assertFalse(ColumnQualificationRewriteRule.of(ambiguousSchema, skip).apply(ambiguousQuery, PG_ANALYZE).rewritten());
    }

    @Test
    void preferred_schema_is_used_for_unqualified_table_resolution() {
        var schema = CatalogSchema.of(
            CatalogTable.of("tenant_a", "users", CatalogColumn.of("tenant_a_id", CatalogType.LONG)),
            CatalogTable.of("tenant_b", "users", CatalogColumn.of("id", CatalogType.LONG))
        );
        var settings = BuiltInRewriteSettings.builder()
            .qualificationDefaultSchema("tenant_b")
            .build();
        var rule = ColumnQualificationRewriteRule.of(schema, settings);
        var query = parseQuery("select id from users u");

        var result = rule.apply(query, PG_ANALYZE);

        assertTrue(result.rewritten());
        assertTrue(SqlStatementRenderer.standard().render(result.statement(), PG_ANALYZE).sql().toLowerCase().contains("u.id"));
    }

    @Test
    void leaves_non_query_statements_unchanged() {
        var schema = CatalogSchema.of(CatalogTable.of("public", "users", CatalogColumn.of("id", CatalogType.LONG)));
        var rule = ColumnQualificationRewriteRule.of(schema, BuiltInRewriteSettings.defaults());
        var statement = insert("users")
            .values(row(lit(1L)))
            .build();

        var result = rule.apply(statement, PG_ANALYZE);

        assertFalse(result.rewritten());
        assertEquals(statement, result.statement());
    }
}


