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
import io.sqm.control.rewrite.SchemaQualificationRewriteRule;
import io.sqm.core.Query;
import io.sqm.core.transform.SchemaQualificationTransformer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SchemaQualificationRewriteRuleTest {
    private static final ExecutionContext PG_ANALYZE = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);

    private static Query parseQuery(String sql) {
        return (Query) SqlStatementParser.standard().parse(sql, PG_ANALYZE);
    }

    @Test
    void id_and_null_arguments_are_validated() {
        var schema = CatalogSchema.of(CatalogTable.of("public", "users", CatalogColumn.of("id", CatalogType.LONG)));
        var rule = SchemaQualificationRewriteRule.of(schema);
        var query = parseQuery("select id from users");

        assertEquals("schema-qualification", rule.id());
        assertThrows(NullPointerException.class, () -> SchemaQualificationRewriteRule.of(null));
        assertThrows(NullPointerException.class, () -> rule.apply(null, PG_ANALYZE));
        assertThrows(NullPointerException.class, () -> rule.apply(query, null));
    }

    @Test
    void unresolved_and_blank_schema_catalog_entries_do_not_rewrite_and_ambiguous_throws() {
        var unresolvedSchema = CatalogSchema.of(
            CatalogTable.of("public", "other", CatalogColumn.of("id", CatalogType.LONG))
        );
        var ambiguousSchema = CatalogSchema.of(
            CatalogTable.of("public", "users", CatalogColumn.of("id", CatalogType.LONG)),
            CatalogTable.of("tenant_a", "users", CatalogColumn.of("id", CatalogType.LONG))
        );
        var blankSchemaName = CatalogSchema.of(
            CatalogTable.of("", "users", CatalogColumn.of("id", CatalogType.LONG))
        );

        var query = parseQuery("select id from users");

        assertFalse(SchemaQualificationRewriteRule.of(unresolvedSchema).apply(query, PG_ANALYZE).rewritten());
        assertThrows(
            SchemaQualificationTransformer.AmbiguousTableQualificationException.class,
            () -> SchemaQualificationRewriteRule.of(ambiguousSchema).apply(query, PG_ANALYZE)
        );
        assertFalse(SchemaQualificationRewriteRule.of(blankSchemaName).apply(query, PG_ANALYZE).rewritten());
    }
}


