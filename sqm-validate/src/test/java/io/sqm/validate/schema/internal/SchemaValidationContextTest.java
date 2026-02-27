package io.sqm.validate.schema.internal;

import io.sqm.catalog.access.CatalogAccessPolicy;
import io.sqm.core.Identifier;
import io.sqm.core.Query;
import io.sqm.core.TableRef;
import io.sqm.core.TypeKeyword;
import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.function.FunctionCatalog;
import io.sqm.validate.schema.function.FunctionSignature;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaValidationContextTest {
    // Touch file to force test recompilation after Dsl.select(...) return-type refactor.
    private static final CatalogSchema SCHEMA = CatalogSchema.of(
        CatalogTable.of("public", "users",
            CatalogColumn.of("id", CatalogType.LONG),
            CatalogColumn.of("name", CatalogType.STRING),
            CatalogColumn.of("age", CatalogType.INTEGER)
        ),
        CatalogTable.of("public", "orders",
            CatalogColumn.of("id", CatalogType.LONG),
            CatalogColumn.of("user_id", CatalogType.LONG)
        )
    );

    private static String contextSourceKey(TableRef tableRef) {
        return new SchemaValidationContext(SCHEMA).sourceKey(tableRef).orElseThrow();
    }

    @Test
    void resolveColumn_reportsAmbiguousForStrictSources() {
        var context = new SchemaValidationContext(SCHEMA);
        context.pushScope();
        try {
            context.registerTableRef(tbl("users").as("u"));
            context.registerTableRef(tbl("orders").as("o"));

            var resolved = context.resolveColumn(col("id"), true);

            assertTrue(resolved.isEmpty());
            assertTrue(context.problems().stream()
                .anyMatch(p -> p.code() == ValidationProblem.Code.COLUMN_AMBIGUOUS));
        } finally {
            context.popScope();
        }
    }

    @Test
    void resolveColumn_skipsErrorWhenOnlyUnknownDerivedSourceIsVisible() {
        var context = new SchemaValidationContext(SCHEMA);
        context.pushScope();
        try {
            Query subquery = select(star()).from(tbl("users").as("u")).build();
            context.registerTableRef(tbl(subquery).as("q"));

            var resolved = context.resolveColumn(col("missing"), true);

            assertTrue(resolved.isEmpty());
            assertTrue(context.problems().isEmpty());
        } finally {
            context.popScope();
        }
    }

    @Test
    void sourceKey_handlesAllSupportedTableRefKinds() {
        var subqueryTable = tbl(select(col("id")).from(tbl("users")).build()).as("sq");
        var valuesTable = tbl(rows(row(lit(1)))).as("v");
        var functionTable = tbl(func("unnest", arg(rows(row(lit(1)))))).as("f");
        TableRef lateral = tbl("users").as("u").lateral();

        assertEquals("sq", contextSourceKey(subqueryTable));
        assertEquals("v", contextSourceKey(valuesTable));
        assertEquals("f", contextSourceKey(functionTable));
        assertEquals("u", contextSourceKey(lateral));
        assertTrue(new SchemaValidationContext(SCHEMA).sourceKey(tbl(select(col("id")).from(tbl("users")).build())).isEmpty());
    }

    @Test
    void inferType_supportsCastArithmeticAndFunctionCatalog() {
        FunctionCatalog catalog = name -> "fnum".equalsIgnoreCase(name)
            ? java.util.Optional.of(FunctionSignature.of(1, 1, CatalogType.DECIMAL))
            : java.util.Optional.empty();
        var context = new SchemaValidationContext(SCHEMA, catalog);

        assertEquals(CatalogType.STRING, context.inferType(lit("x").cast(type(TypeKeyword.CHARACTER_VARYING))).orElseThrow());
        assertEquals(CatalogType.INTEGER, context.inferType(lit("1").cast(type("int4"))).orElseThrow());
        assertEquals(CatalogType.LONG, context.inferType(lit(1).add(lit(2L))).orElseThrow());
        assertEquals(CatalogType.DECIMAL, context.inferType(lit(1L).add(lit(2.5))).orElseThrow());
        assertEquals(CatalogType.INTEGER, context.inferType(lit(1).neg()).orElseThrow());
        assertTrue(context.inferType(lit("bad").neg()).isEmpty());
        assertEquals(CatalogType.DECIMAL, context.inferType(func("fnum", arg(lit(1)))).orElseThrow());
    }

    @Test
    void inferProjectionTypes_returnsEmptyForNonExpressionProjection() {
        var context = new SchemaValidationContext(SCHEMA);
        var projection = context.inferProjectionTypes(select(star()).from(tbl("users")).build());
        assertTrue(projection.isEmpty());
    }

    @Test
    void onJoinVisibleAliases_areNormalizedAndDefaultToEmpty() {
        var context = new SchemaValidationContext(SCHEMA);
        var join = inner(tbl("orders").as("o")).on(col("o", "user_id").eq(col("u", "id")));

        assertTrue(context.onJoinVisibleAliases(join).isEmpty());

        context.pushScope();
        try {
            context.registerOnJoinVisibleAliases(join, Set.of("U", "o"));
            var aliases = context.onJoinVisibleAliases(join);
            assertTrue(aliases.contains("u"));
            assertTrue(aliases.contains("o"));
        } finally {
            context.popScope();
        }
    }

    @Test
    void identifierOverloadsAndCurrentScopeHelpersWork() {
        var context = new SchemaValidationContext(SCHEMA);
        context.pushScope();
        try {
            context.registerTableRef(tbl("users").as("u"));
            context.registerTableRef(tbl(rows(row(lit(1)))).as("v").columnAliases("c1"));

            assertEquals(1, context.countStrictSourcesWithColumn(Identifier.of("id"), null));
            assertEquals(0, context.countStrictSourcesWithColumn(Identifier.of("id"), "u"));
            assertEquals(CatalogType.LONG, context.sourceColumnType("u", Identifier.of("id")).orElseThrow());
            assertTrue(context.sourceColumnType("v", Identifier.of("c1")).isEmpty()); // derived sources are non-strict
            assertEquals(Set.of("u", "v"), Set.copyOf(context.currentScopeSourceKeys()));
        } finally {
            context.popScope();
        }
    }

    @Test
    void accessPolicyIdentifierOverloadsDelegateWithPrincipal() {
        CatalogAccessPolicy policy = new CatalogAccessPolicy() {
            @Override
            public boolean isTableDenied(String principal, String schemaName, String tableName) {
                return "p1".equals(principal) && "public".equals(schemaName) && "users".equals(tableName);
            }

            @Override
            public boolean isColumnDenied(String principal, String sourceName, String columnName) {
                return "p1".equals(principal) && "u".equals(sourceName) && "secret".equals(columnName);
            }

            @Override
            public boolean isFunctionAllowed(String principal, String functionName) {
                return true;
            }
        };
        var context = new SchemaValidationContext(SCHEMA, name -> java.util.Optional.empty(), policy, "p1");

        assertTrue(context.isTableDenied("public", "users"));
        assertTrue(context.isColumnDenied(Identifier.of("u"), Identifier.of("secret")));
        assertFalse(context.isColumnDenied(null, Identifier.of("secret")));
    }

    @Test
    void accessPolicyOverloadsDelegateWithTenantAndPrincipal() {
        CatalogAccessPolicy policy = new CatalogAccessPolicy() {
            @Override
            public boolean isTableDenied(String principal, String schemaName, String tableName) {
                return false;
            }

            @Override
            public boolean isColumnDenied(String principal, String sourceName, String columnName) {
                return false;
            }

            @Override
            public boolean isFunctionAllowed(String principal, String functionName) {
                return false;
            }

            @Override
            public boolean isTableDenied(String tenant, String principal, String schemaName, String tableName) {
                return "t1".equals(tenant) && "p1".equals(principal) && "users".equals(tableName);
            }

            @Override
            public boolean isColumnDenied(String tenant, String principal, String sourceName, String columnName) {
                return "t1".equals(tenant) && "p1".equals(principal) && "u".equals(sourceName) && "secret".equals(columnName);
            }

            @Override
            public boolean isFunctionAllowed(String tenant, String principal, String functionName) {
                return "t1".equals(tenant) && "p1".equals(principal) && "length".equals(functionName);
            }
        };
        var context = new SchemaValidationContext(SCHEMA, name -> java.util.Optional.empty(), policy, "t1", "p1");

        assertTrue(context.isTableDenied("public", "users"));
        assertTrue(context.isColumnDenied(Identifier.of("u"), Identifier.of("secret")));
        assertTrue(context.isFunctionAllowed("length"));
    }

    @Test
    void normalizeIdentifierRejectsNullIdentifier() {
        //noinspection DataFlowIssue
        assertThrows(NullPointerException.class, () -> SchemaValidationContext.normalize((Identifier) null));
    }
}

