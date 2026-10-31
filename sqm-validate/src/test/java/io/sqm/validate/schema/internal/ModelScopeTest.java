package io.sqm.validate.schema.internal;

import io.sqm.catalog.model.*;
import io.sqm.core.*;
import io.sqm.validate.schema.SchemaStatementValidator;
import io.sqm.validate.schema.SchemaValidationSettings;
import io.sqm.validate.schema.rule.ModelClauseFeatureValidationRule;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class ModelScopeTest {
    private static final CatalogSchema SCHEMA = CatalogSchema.of(CatalogTable.of("public", "data",
        CatalogColumn.of("p", CatalogType.STRING), CatalogColumn.of("y", CatalogType.INTEGER), CatalogColumn.of("amount", CatalogType.DECIMAL)));
    private static final SchemaValidationSettings SETTINGS = SchemaValidationSettings.builder()
        .addRules(List.of(new ModelClauseFeatureValidationRule("native", true))).build();

    @Test void snapshotsResolveMainReferenceAndQuotedNamesWithoutInputFallback() {
        var context = new SchemaValidationContext(SCHEMA);
        context.pushScope();
        context.registerTableRef(tbl("data"));
        var ref = referenceModel("growth", select(col("p"), col("amount").as("rate")).from(tbl("data")).build())
            .dimension("p").measure("rate").build();
        var clause = model().reference(ref).main(mainModel().name("forecast").partition("p").dimension("y").measure("amount", "sales")
            .rule(cellTarget("sales", 1), lit(1)).build()).build();
        var scope = context.modelScope(clause);
        assertEquals(CatalogType.DECIMAL, scope.resolve(null, id("sales")).type());
        assertEquals(CatalogType.DECIMAL, scope.resolve(id("FORECAST"), id("sales")).type());
        assertEquals(CatalogType.DECIMAL, scope.resolve(id("growth"), id("rate")).type());
        assertNull(scope.resolve(id("unknown"), id("rate")));
        assertNull(scope.resolve(null, id("amount")));
        assertNull(scope.resolve(null, id("sales", QuoteStyle.DOUBLE_QUOTE)));
        assertEquals("sales", ModelScope.key(id("sales", QuoteStyle.DOUBLE_QUOTE)));
        assertThrows(UnsupportedOperationException.class, () -> scope.columns().put("X", CatalogColumn.of("x", CatalogType.STRING)));
        assertThrows(UnsupportedOperationException.class, () -> scope.references().get("GROWTH").clear());
        context.pushDeclaredColumns(scope.columns(), "model.rules");
        context.pushModelScope(scope);
        assertSame(scope, context.modelScope());
        assertEquals(CatalogType.DECIMAL, context.inferType(cellRef("sales", 1)).orElseThrow());
        assertEquals(CatalogType.DECIMAL, context.inferType(modelAggregateExpr(func("sum", col("sales")), List.of(cellAny()))).orElseThrow());
        assertEquals(CatalogType.INTEGER, context.inferType(currentDimension("y")).orElseThrow());
        assertTrue(context.inferType(currentDimension()).isEmpty());
        assertEquals(CatalogType.INTEGER, context.inferType(iterationNumber()).orElseThrow());
        assertEquals(CatalogType.DECIMAL, context.inferType(previousModelValueExpr(cellRef("sales", 1))).orElseThrow());
        assertEquals(CatalogType.INTEGER, context.inferType(presenceValue(cellRef("sales", 1), lit(1), lit(0))).orElseThrow());
        context.pushScope();
        assertNull(context.modelScope());
        context.popScope();
        context.popModelScope();
        context.popDeclaredColumns();
        context.popScope();
        assertNull(context.modelScope());
    }

    @Test void closedOutputsRetainDeclarationOrderAliasesAndProjectionTypesThroughWithAndSetQueries() {
        var context = new SchemaValidationContext(SCHEMA);
        var source = select(star()).from(tbl("data")).model(model().dimension("p").dimension("y").measure("amount", "sales")
            .rule(cellTarget("sales", "Bike", 1), lit(1)).build()).build();
        var columns = context.queryOutputColumns(source);
        assertEquals(List.of("P", "Y", "SALES"), List.copyOf(columns.keySet()));
        assertEquals(CatalogType.DECIMAL, columns.get("SALES").type());
        assertThrows(UnsupportedOperationException.class, columns::clear);
        assertEquals(columns, context.queryOutputColumns(source.unionAll(source)));
        var withQuery = with(cte("forecast", source)).body(select(star()).from(tbl("forecast")).build());
        assertEquals(columns, context.queryOutputColumns(withQuery));
        assertEquals(List.of("P", "Y", "AMOUNT"), List.copyOf(context.queryOutputColumns(select(star()).from(tbl("data")).build()).keySet()));
        var renamed = select(col("sales")).from(tbl("data")).model(source.model()).build();
        context.pushScope();
        context.registerTableRef(tbl(renamed).as("q").columnAliases("total"));
        assertEquals(CatalogType.DECIMAL, context.resolveColumn(col("q", "total"), true).orElseThrow().type());
        assertTrue(context.resolveColumn(col("q", "amount"), true).isEmpty());
        context.popScope();
        assertEquals(CatalogType.DECIMAL, context.inferProjectionTypes(renamed).orElseThrow().getFirst().orElseThrow());
        context.pushWithScope();
        context.registerCte(cte("renamed", renamed).columnAliases("total"));
        assertTrue(context.isCteReference(tbl("renamed")));
        assertFalse(context.isCteReference(tbl("public", "renamed")));
        context.pushScope();
        context.registerTableRef(tbl("renamed"));
        assertEquals(CatalogType.DECIMAL, context.inferType(col("total")).orElseThrow());
        context.popScope();
        context.popWithScope();
        context.pushWithScope();
        context.registerCte(cte("constants", select(lit(1), lit("Bike")).build()).columnAliases("n", "label"));
        context.pushScope();
        context.registerTableRef(tbl("constants"));
        assertEquals(CatalogType.INTEGER, context.inferType(col("n")).orElseThrow());
        assertEquals(CatalogType.STRING, context.inferType(col("label")).orElseThrow());
        context.popScope();
        context.popWithScope();
    }

    @Test void queryBarriersDetectCorrelationAndRestoreOuterScopeWithoutRejectingShadowing() {
        var context = new SchemaValidationContext(SCHEMA);
        context.pushScope();
        context.registerTableRef(tbl("data").as("outer_data"));
        context.pushQueryIsolation("model.reference.query");
        context.pushScope();
        context.registerTableRef(tbl("data").as("local_data"));
        assertTrue(context.resolveColumn(col("outer_data", "amount"), true).isEmpty());
        assertEquals("model.reference.query", context.problems().getFirst().clausePath());
        assertTrue(context.resolveColumn(col("local_data", "amount"), true).isPresent());
        context.popScope();
        context.popQueryIsolation();
        assertTrue(context.resolveColumn(col("outer_data", "amount"), true).isPresent());
        context.popScope();
    }

    @Test void schemaTraversalVisitsAllModelExpressionsAndIsolatesNestedOrdinaryQueries() {
        var cell = cellRef("sales", 1);
        var rules = modelRules().iteration(iterationSpec(lit(2), previousModelValueExpr(cell).eq(cell)))
            .rule(cellTarget("sales", cellForValues("y", select(col("y")).from(tbl("data")).build())),
                presenceNonNullValue(cell, modelAggregateExpr(func("sum", col("sales")), List.of(cellCondition(isAny("y")))), iterationNumber()))
            .rule(cellTarget("sales", 2), presenceValue(cell, currentDimension("y"), lit(0)))
            .rule(cellTarget("sales", 3), cellIsPresent(cell)).build();
        var ref = referenceModel("growth", select(col("p"), col("amount").as("rate")).from(tbl("data")).build()).dimension("p").measure("rate").build();
        var query = select(col("sales")).from(tbl("data")).model(model().reference(ref).dimension("y").measure("amount", "sales").rules(rules).build()).build();
        var result = SchemaStatementValidator.of(SCHEMA, SETTINGS).validate(query);
        assertTrue(result.ok(), () -> result.problems().toString());
        var nested = select(col("sales")).from(tbl("data")).model(model().dimension("y").measure("amount", "sales")
            .rule(cellTarget("sales", 1), expr(select(cell).build())).build()).build();
        assertTrue(SchemaStatementValidator.of(SCHEMA, SETTINGS).validate(nested).problems().stream()
            .anyMatch(p -> p.clausePath().equals("expression.model")));
    }

    @Test void unknownReferenceStarExpansionsDeferResolutionWhileKnownStarsRemainStrict() {
        var ref = referenceModel("growth", select(star()).from(tbl("data").as("d")).build())
            .dimension("y").measure("amount").build();
        var query = select(col("sales")).from(tbl("data")).model(model().reference(ref).dimension("y").measure("amount", "sales")
            .rule(cellTarget("sales", 1), lit(1)).build()).build();
        var permissive = SchemaStatementValidator.of(CatalogSchema.allowEverything(), SETTINGS).validate(query);
        assertTrue(permissive.ok(), () -> permissive.problems().toString());
        assertTrue(SchemaStatementValidator.of(SCHEMA, SETTINGS).validate(query).ok());
        var context = new SchemaValidationContext(SCHEMA);
        assertEquals(3, context.queryOutputColumns(select(star("d")).from(tbl("data").as("d")).build()).size());
        assertTrue(context.queryOutputColumns(select(star("missing")).from(tbl("data")).build()).isEmpty());
        var quoted = select(star()).model(model().dimension(lit(1), "y")
            .measure(modelColumn(lit(0), id("Sales", QuoteStyle.DOUBLE_QUOTE)))
            .rule(cellTarget(id("Sales", QuoteStyle.DOUBLE_QUOTE)).address(1).build(), lit(1)).build()).build();
        var cteQuery = with(cte("q", quoted)).body(select(star()).from(tbl("q")).build());
        assertTrue(context.queryOutputColumns(cteQuery).containsKey("Sales"));
        assertFalse(context.queryOutputColumns(cteQuery).containsKey("SALES"));
    }
}
