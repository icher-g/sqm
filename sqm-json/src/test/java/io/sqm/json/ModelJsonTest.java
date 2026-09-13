package io.sqm.json;

import io.sqm.core.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class ModelJsonTest {
    @Test
    void fluentCellBuildersProduceTheSameSerializableNodes() throws Exception {
        var mapper = SqmJsonMixins.createDefault();
        var query = select(col("sales")).from(tbl("sales_data"))
            .model(model().dimension("product").dimension("year").measure("sales")
                .rule(cellTarget("sales").address("Bike").address(cellForValues("year", 2026, 2027)).build(),
                    cellRef("sales").selector("Bike").selector(currentDimension("year")).build())
                .build()).build();
        assertEquals(query, mapper.readValue(mapper.writeValueAsString(query), Query.class));
        var qualified = cellRef("sales").model("baseline").selector(cellValue("Bike")).selector(2025).build();
        assertEquals(qualified, mapper.readValue(mapper.writeValueAsString(qualified), CellRefExpr.class));
    }

    @Test
    void sharedModelColumnsRoundTripWithRolesPreservedByTheirContainingLists() throws Exception {
        var mapper = SqmJsonMixins.createDefault();
        var clause = model().partition("country").dimension("sales_year", "year")
            .measure("amount", "sales")
            .reference(referenceModel("baseline", select(col("sales_year"), col("amount")).from(tbl("history")).build())
                .dimension("sales_year", "year").measure("amount", "sales").build())
            .rule(cellTarget("sales", 2026), lit(100)).build();
        var tree = mapper.valueToTree(clause);
        for (var role : List.of("partitions", "dimensions", "measures")) {
            assertEquals("model-column", tree.path("main").path(role).get(0).path("kind").asText());
        }
        assertEquals(clause, mapper.treeToValue(tree, ModelClause.class));
        for (var column : List.of(clause.main().partitions().getFirst(),
            clause.main().dimensions().getFirst(), clause.main().measures().getFirst())) {
            var json = mapper.writeValueAsString(column);
            assertEquals(column, mapper.readValue(json, ModelColumn.class));
            assertEquals(column, mapper.readValue(json, Node.class));
        }
    }

    @Test
    void nullableModelPropertiesRoundTripWhenOmittedOrExplicitlyNull() throws Exception {
        var mapper = SqmJsonMixins.createDefault();
        var main = mainModel().dimension("year").measure("sales")
            .rule(cellTarget("sales", 2026), lit(100)).build();
        var cases = List.of(
            Map.entry((Node) main, List.of("name")),
            Map.entry((Node) main.rules(), List.of("iteration")),
            Map.entry((Node) main.rules().rules().getFirst(), List.of("mode", "orderBy")),
            Map.entry((Node) iterationSpec(lit(10)), List.of("until")),
            Map.entry((Node) cellRef("sales", 2026), List.of("model")),
            Map.entry((Node) currentDimension(), List.of("dimension")),
            Map.entry((Node) isAny(), List.of("dimension")),
            Map.entry((Node) cellForRange("year", 2026, 2030, RangeDirection.INCREMENT, 1), List.of("likePattern")));
        for (var entry : cases) {
            var node = entry.getKey();
            var tree = mapper.valueToTree(node);
            assertEquals(node, mapper.treeToValue(tree, Node.class));
            var object = (com.fasterxml.jackson.databind.node.ObjectNode) tree;
            entry.getValue().forEach(object::remove);
            assertEquals(node, mapper.treeToValue(object, Node.class));
            entry.getValue().forEach(object::putNull);
            assertEquals(node, mapper.treeToValue(object, Node.class));
        }
    }

    @Test
    void forValuesRoundTripWithEverySharedSourceAndOneGeneratorDiscriminator() throws Exception {
        var mapper = SqmJsonMixins.createDefault();
        for (var generator : List.of(
            cellForValues("year", 2026, 2027),
            cellForValues(List.of(id("product"), id("year")), rows(row("Bike", 2026))),
            cellForValues("year", select(col("forecast_year")).from(tbl("forecasts")).build()))) {
            var json = mapper.writeValueAsString(generator);
            assertEquals("cell-for-values", mapper.readTree(json).get("kind").asText());
            assertTrue(mapper.readTree(json).has("values"));
            for (var root : List.of(Node.class, CellAddress.class, CellFor.class, CellFor.Values.class)) {
                assertEquals(generator, mapper.readValue(json, root));
            }
            var calculation = model().measure("sales");
            generator.dimensions().forEach(dimension -> calculation.dimension(col(dimension), dimension));
            var query = select(col("sales")).from(tbl("input"))
                .model(calculation.rule(cellTarget("sales", generator), lit(100)).build()).build();
            assertEquals(query, mapper.readValue(mapper.writeValueAsString(query), Query.class));
        }
    }

    @Test
    void reusedPredicatesAndBothWildcardFormsRoundTripInCellSelectors() throws Exception {
        var mapper = SqmJsonMixins.createDefault();
        var year = col(id("Sales Year", QuoteStyle.DOUBLE_QUOTE));
        for (Predicate predicate : List.of(year.eq(2026), year.between(2023, 2026),
            year.between(2020, 2021).negated(true), year.in(2025, 2026), year.notIn(2020, 2021),
            year.isNull(), year.isNotNull(), year.gt(2020).and(year.lt(2027)),
            year.eq(2025).or(year.eq(2026)), not(year.eq(2024)), year.isAny(), isAny())) {
            var selector = cellCondition(predicate);
            var json = mapper.writeValueAsString(selector);
            assertEquals(selector, mapper.readValue(json, CellSelector.class));
            assertEquals(selector, mapper.readValue(json, CellAddress.class));
            assertEquals(selector, mapper.readValue(json, Node.class));
            assertTrue(mapper.readTree(json).has("predicate"));
            assertFalse(mapper.readTree(json).has("dimension"));
        }
        for (var wildcard : List.of(isAny(), isAny(year))) {
            var json = mapper.writeValueAsString(wildcard);
            assertEquals(wildcard, mapper.readValue(json, IsAnyPredicate.class));
            assertEquals(wildcard, mapper.readValue(json, Predicate.class));
            assertEquals(wildcard, mapper.readValue(json, Expression.class));
            assertEquals(wildcard, mapper.readValue(json, Node.class));
            assertEquals("is-any", mapper.readTree(json).get("kind").asText());
        }
    }

    @Test
    void allModelNodesRoundTripThroughTheirTypedRoots() throws Exception {
        var mapper = SqmJsonMixins.createDefault();
        var ModelColumnNode = modelColumn(lit(10), "sales");
        var ModelColumnJson = mapper.writeValueAsString(ModelColumnNode);
        assertEquals(ModelColumnNode, mapper.readValue(ModelColumnJson, ModelColumn.class));
        assertEquals(ModelColumnNode, mapper.readValue(ModelColumnJson, Node.class));
        var CellRefExprNode = cellRef("sales", 2026);
        var CellRefExprJson = mapper.writeValueAsString(CellRefExprNode);
        assertEquals("cell-ref-expr", mapper.readTree(CellRefExprJson).path("kind").asText());
        assertEquals(CellRefExprNode, mapper.readValue(CellRefExprJson, CellRefExpr.class));
        assertEquals(CellRefExprNode, mapper.readValue(CellRefExprJson, Node.class));
        assertEquals(CellRefExprNode, mapper.readValue(CellRefExprJson, Expression.class));
        var CellTargetNode = cellTarget("sales", 2026);
        var CellTargetJson = mapper.writeValueAsString(CellTargetNode);
        assertEquals("cell-target", mapper.readTree(CellTargetJson).path("kind").asText());
        assertEquals(CellTargetNode, mapper.readValue(CellTargetJson, CellTarget.class));
        assertEquals(CellTargetNode, mapper.readValue(CellTargetJson, Node.class));
        var ModelAggregateExprNode = modelAggregateExpr(func("SUM", col("sales")), List.of(cellAny()));
        var ModelAggregateExprJson = mapper.writeValueAsString(ModelAggregateExprNode);
        assertEquals(ModelAggregateExprNode, mapper.readValue(ModelAggregateExprJson, ModelAggregateExpr.class));
        assertEquals(ModelAggregateExprNode, mapper.readValue(ModelAggregateExprJson, Node.class));
        assertEquals(ModelAggregateExprNode, mapper.readValue(ModelAggregateExprJson, Expression.class));
        var CurrentDimensionExprNode = currentDimension("year");
        var CurrentDimensionExprJson = mapper.writeValueAsString(CurrentDimensionExprNode);
        assertEquals(CurrentDimensionExprNode, mapper.readValue(CurrentDimensionExprJson, CurrentDimensionExpr.class));
        assertEquals(CurrentDimensionExprNode, mapper.readValue(CurrentDimensionExprJson, Node.class));
        assertEquals(CurrentDimensionExprNode, mapper.readValue(CurrentDimensionExprJson, Expression.class));
        var IterationNumberExprNode = iterationNumber();
        var IterationNumberExprJson = mapper.writeValueAsString(IterationNumberExprNode);
        assertEquals(IterationNumberExprNode, mapper.readValue(IterationNumberExprJson, IterationNumberExpr.class));
        assertEquals(IterationNumberExprNode, mapper.readValue(IterationNumberExprJson, Node.class));
        assertEquals(IterationNumberExprNode, mapper.readValue(IterationNumberExprJson, Expression.class));
        var PreviousModelValueExprNode = previousModelValueExpr(cellRef("sales", 2026));
        var PreviousModelValueExprJson = mapper.writeValueAsString(PreviousModelValueExprNode);
        assertEquals(PreviousModelValueExprNode, mapper.readValue(PreviousModelValueExprJson, PreviousModelValueExpr.class));
        assertEquals(PreviousModelValueExprNode, mapper.readValue(PreviousModelValueExprJson, Node.class));
        assertEquals(PreviousModelValueExprNode, mapper.readValue(PreviousModelValueExprJson, Expression.class));
        var PresenceValueExprNode = presenceValue(cellRef("sales", 2026), lit(10), lit(0));
        var PresenceValueExprJson = mapper.writeValueAsString(PresenceValueExprNode);
        assertEquals(PresenceValueExprNode, mapper.readValue(PresenceValueExprJson, PresenceValueExpr.class));
        assertEquals(PresenceValueExprNode, mapper.readValue(PresenceValueExprJson, Node.class));
        assertEquals(PresenceValueExprNode, mapper.readValue(PresenceValueExprJson, Expression.class));
        var CellPresentPredicateNode = cellIsPresent(cellRef("sales", 2026));
        var CellPresentPredicateJson = mapper.writeValueAsString(CellPresentPredicateNode);
        assertEquals(CellPresentPredicateNode, mapper.readValue(CellPresentPredicateJson, CellPresentPredicate.class));
        assertEquals(CellPresentPredicateNode, mapper.readValue(CellPresentPredicateJson, Node.class));
        assertEquals(CellPresentPredicateNode, mapper.readValue(CellPresentPredicateJson, Expression.class));
        var IterationSpecNode = iterationSpec(lit(10), cellRef("sales", 2026).gt(lit(0)));
        var IterationSpecJson = mapper.writeValueAsString(IterationSpecNode);
        assertEquals(IterationSpecNode, mapper.readValue(IterationSpecJson, IterationSpec.class));
        assertEquals(IterationSpecNode, mapper.readValue(IterationSpecJson, Node.class));
        var ModelRuleNode = modelRule(RuleMode.UPDATE, cellTarget("sales", 2026), OrderBy.of(List.of(col("year").asc())), lit(20));
        var ModelRuleJson = mapper.writeValueAsString(ModelRuleNode);
        assertEquals(ModelRuleNode, mapper.readValue(ModelRuleJson, ModelRule.class));
        assertEquals(ModelRuleNode, mapper.readValue(ModelRuleJson, Node.class));
        var ModelRulesNode = modelRules().rule(cellTarget("sales", 2026), lit(20)).iteration(iterationSpec(lit(2), null)).build();
        var ModelRulesJson = mapper.writeValueAsString(ModelRulesNode);
        assertEquals(ModelRulesNode, mapper.readValue(ModelRulesJson, ModelRules.class));
        assertEquals(ModelRulesNode, mapper.readValue(ModelRulesJson, Node.class));
        var MainModelNode = mainModel().dimension("year").measure(lit(10), "sales").rule(cellTarget("sales", 2026), lit(20)).build();
        var MainModelJson = mapper.writeValueAsString(MainModelNode);
        assertEquals(MainModelNode, mapper.readValue(MainModelJson, MainModel.class));
        assertEquals(MainModelNode, mapper.readValue(MainModelJson, Node.class));
        var ReferenceModelNode = referenceModel("rates", select(lit(1)).build()).dimension("year").measure(lit(10), "rate").build();
        var ReferenceModelJson = mapper.writeValueAsString(ReferenceModelNode);
        assertEquals(ReferenceModelNode, mapper.readValue(ReferenceModelJson, ReferenceModel.class));
        assertEquals(ReferenceModelNode, mapper.readValue(ReferenceModelJson, Node.class));
        var ModelClauseNode = model().dimension("year").measure(lit(10), "sales").reference(referenceModel("rates", select(lit(1)).build()).dimension("year").measure("rate").build()).rule(cellTarget("sales", 2026), lit(20)).build();
        var ModelClauseJson = mapper.writeValueAsString(ModelClauseNode);
        assertEquals(ModelClauseNode, mapper.readValue(ModelClauseJson, ModelClause.class));
        assertEquals(ModelClauseNode, mapper.readValue(ModelClauseJson, Node.class));
        var CellSelectorValueNode = cellValue(lit(2026));
        var CellSelectorValueJson = mapper.writeValueAsString(CellSelectorValueNode);
        assertEquals(CellSelectorValueNode, mapper.readValue(CellSelectorValueJson, CellSelector.class));
        assertEquals(CellSelectorValueNode, mapper.readValue(CellSelectorValueJson, Node.class));
        assertEquals(CellSelectorValueNode, mapper.readValue(CellSelectorValueJson, CellAddress.class));
        var CellSelectorConditionNode = cellCondition(col("year").gt(2020));
        var CellSelectorConditionJson = mapper.writeValueAsString(CellSelectorConditionNode);
        assertEquals(CellSelectorConditionNode, mapper.readValue(CellSelectorConditionJson, CellSelector.class));
        assertEquals(CellSelectorConditionNode, mapper.readValue(CellSelectorConditionJson, Node.class));
        assertEquals(CellSelectorConditionNode, mapper.readValue(CellSelectorConditionJson, CellAddress.class));
        var IsAnyPredicateNode = isAny("year");
        var IsAnyPredicateJson = mapper.writeValueAsString(IsAnyPredicateNode);
        assertEquals(IsAnyPredicateNode, mapper.readValue(IsAnyPredicateJson, Predicate.class));
        assertEquals(IsAnyPredicateNode, mapper.readValue(IsAnyPredicateJson, Node.class));
        assertEquals(IsAnyPredicateNode, mapper.readValue(IsAnyPredicateJson, Expression.class));
        var CellForValuesNode = cellForValues(List.of(id("year"), id("product")), rows(row(2026, "Bike")));
        var CellForValuesJson = mapper.writeValueAsString(CellForValuesNode);
        assertEquals(CellForValuesNode, mapper.readValue(CellForValuesJson, CellFor.class));
        assertEquals(CellForValuesNode, mapper.readValue(CellForValuesJson, Node.class));
        assertEquals(CellForValuesNode, mapper.readValue(CellForValuesJson, CellAddress.class));
        var queryGenerator = cellForValues(List.of(id("year")), select(lit(2026)).build());
        var queryGeneratorJson = mapper.writeValueAsString(queryGenerator);
        assertEquals(queryGenerator, mapper.readValue(queryGeneratorJson, CellFor.class));
        assertEquals(queryGenerator, mapper.readValue(queryGeneratorJson, Node.class));
        assertEquals(queryGenerator, mapper.readValue(queryGeneratorJson, CellAddress.class));
        var CellForRangeNode = cellForRange(id("year"), lit("Y%"), lit(2020), lit(2026), RangeDirection.INCREMENT, lit(1));
        var CellForRangeJson = mapper.writeValueAsString(CellForRangeNode);
        assertEquals(CellForRangeNode, mapper.readValue(CellForRangeJson, CellFor.class));
        assertEquals(CellForRangeNode, mapper.readValue(CellForRangeJson, Node.class));
        assertEquals(CellForRangeNode, mapper.readValue(CellForRangeJson, CellAddress.class));
    }

    @Test
    void queryWithQuotedModelNamesRoundTripsAndLegacyQueryHasNoModel() throws Exception {
        var mapper = SqmJsonMixins.createDefault();
        var quoted = id("Sales Year", QuoteStyle.DOUBLE_QUOTE);
        var clause = model().dimension(col("source_year"), quoted).measure("amount", "sales")
            .rule(cellTarget("sales", 2026), presenceNonNullValue(cellRef("sales", 2025), lit(10), lit(0)))
            .build();
        var query = select(col("sales")).from(tbl("source")).model(clause).build();
        String json = mapper.writeValueAsString(query);
        assertEquals(query, mapper.readValue(json, Query.class));
        assertTrue(json.contains("Sales Year"));
        var legacy = mapper.valueToTree(SelectQuery.builder(query).clearModel().build());
        assertNull(mapper.treeToValue(legacy, SelectQuery.class).model());
        assertFalse(legacy.has("model"));
    }
}
