package io.sqm.validate.schema.internal;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaValidationContextTest {
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
            Query subquery = select(star()).from(tbl("users").as("u"));
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
        var subqueryTable = tbl(select(col("id")).from(tbl("users"))).as("sq");
        var valuesTable = tbl(rows(row(lit(1)))).as("v");
        var functionTable = tbl(func("unnest", arg(rows(row(lit(1)))))).as("f");
        TableRef lateral = tbl("users").as("u").lateral();

        assertEquals("sq", contextSourceKey(subqueryTable));
        assertEquals("v", contextSourceKey(valuesTable));
        assertEquals("f", contextSourceKey(functionTable));
        assertEquals("u", contextSourceKey(lateral));
        assertTrue(new SchemaValidationContext(SCHEMA).sourceKey(tbl(select(col("id")).from(tbl("users")))).isEmpty());
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
        var projection = context.inferProjectionTypes(select(star()).from(tbl("users")));
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
}

