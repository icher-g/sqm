package io.sqm.parser.oracle;

import io.sqm.core.*;
import io.sqm.parser.ansi.AnsiSpecs;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.ParserException;
import io.sqm.parser.oracle.spi.OracleSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.sqm.dsl.Dsl.*;
import static io.sqm.parser.ansi.Parsers.ansiCopy;
import static org.junit.jupiter.api.Assertions.*;

class ModelParserRegistrationTest {
    private static final List<Class<? extends Node>> TYPES = List.of(
        ModelClause.class,
        MainModel.class,
        ReferenceModel.class,
        ModelColumn.class,
        ModelRules.class,
        ModelRule.class,
        IterationSpec.class,
        CellTarget.class,
        CellRefExpr.class,
        CellAddress.class,
        CellSelector.class,
        CellSelector.Value.class,
        CellSelector.Condition.class,
        CellFor.class,
        CellFor.Values.class,
        CellFor.Range.class,
        ModelAggregateExpr.class,
        CurrentDimensionExpr.class,
        IterationNumberExpr.class,
        PreviousModelValueExpr.class,
        PresenceValueExpr.class,
        CellPresentPredicate.class,
        IsAnyPredicate.class);
    private static final Map<Class<? extends Node>, String> BASIC = Map.ofEntries(
        Map.entry(ModelClause.class, "MODEL DIMENSION BY (y) MEASURES (sales) (sales[1] = 1)"),
        Map.entry(MainModel.class, "DIMENSION BY (y) MEASURES (sales) (sales[1] = 1)"),
        Map.entry(ModelColumn.class, "amount AS sales"),
        Map.entry(ModelRules.class, "RULES (sales[1] = 1)"),
        Map.entry(ModelRule.class, "sales[1] = 1"),
        Map.entry(CellTarget.class, "sales[1]"),
        Map.entry(CellRefExpr.class, "sales[1]"),
        Map.entry(CellAddress.class, "1"),
        Map.entry(CellSelector.class, "1"),
        Map.entry(CellSelector.Value.class, "1"),
        Map.entry(ReferenceModel.class, "REFERENCE baseline ON (SELECT y, sales FROM t) DIMENSION BY (y) MEASURES (sales)"),
        Map.entry(CellSelector.Condition.class, "y BETWEEN 1 AND 2"),
        Map.entry(CellFor.class, "FOR y IN (1, 2)"),
        Map.entry(CellFor.Values.class, "FOR y IN (1, 2)"),
        Map.entry(CellFor.Range.class, "FOR y FROM 1 TO 2 INCREMENT 1"),
        Map.entry(ModelAggregateExpr.class, "SUM(sales)[ANY]"),
        Map.entry(IterationSpec.class, "ITERATE (2)"),
        Map.entry(CurrentDimensionExpr.class, "CV(y)"),
        Map.entry(IterationNumberExpr.class, "ITERATION_NUMBER"),
        Map.entry(PreviousModelValueExpr.class, "PREVIOUS(sales[1])"),
        Map.entry(PresenceValueExpr.class, "PRESENTV(sales[1], 1, 0)"),
        Map.entry(CellPresentPredicate.class, "sales[1] IS PRESENT"),
        Map.entry(IsAnyPredicate.class, "y IS ANY"));

    @Test
    void everyNodeHasAnExplicitAnsiAndOracleBoundary() {
        var ansi = ParseContext.of(new AnsiSpecs());
        var oracle = ParseContext.of(new OracleSpecs());
        for (var type : TYPES) {
            assertNotNull(ansiCopy().require(type));
            assertNotNull(Parsers.oracleCopy().require(type));
            var result = ansi.parse(type, "anything");
            assertTrue(result.isError(), type.getSimpleName());
            assertTrue(Objects.requireNonNull(result.errorMessage()).contains("MODEL"), result::errorMessage);
            assertEquals("io.sqm.parser.oracle", Parsers.oracle().require(type).getClass().getPackageName());
        }
    }

    @Test
    void directBasicParsersHonorCapabilitiesAndParseFragments() {
        var ansi = ParseContext.of(new AnsiSpecs());
        var oracle = ParseContext.of(new OracleSpecs());
        for (var entry : BASIC.entrySet()) {
            var handler = Parsers.oracle().require(entry.getKey());
            var denied = ansi.parse(handler, entry.getValue());
            assertTrue(denied.isError(), entry.getKey().getSimpleName());
            assertTrue(Objects.requireNonNull(denied.errorMessage()).contains("not supported"), denied::errorMessage);
            var parsed = oracle.parse(entry.getKey(), entry.getValue());
            assertTrue(parsed.ok(), parsed::errorMessage);
        }
    }

    @Test
    void localScopeIsImmutableAndPreservesIdentifierCaseRules() {
        var scope = ModelParseScope.of(List.of(modelColumn("sales"),
            modelColumn(col("amount"), id("Exact", QuoteStyle.DOUBLE_QUOTE))));
        assertTrue(scope.contains(id("SALES")));
        assertTrue(scope.contains(id("sales")));
        assertTrue(scope.contains(id("SALES", QuoteStyle.DOUBLE_QUOTE)));
        assertFalse(scope.contains(id("sales", QuoteStyle.DOUBLE_QUOTE)));
        assertTrue(scope.contains(id("Exact", QuoteStyle.DOUBLE_QUOTE)));
        assertFalse(scope.contains(id("Exact")));
        assertThrows(UnsupportedOperationException.class, () -> scope.measures().clear());
        var parent = ParseContext.of(new OracleSpecs());
        var local = new ModelParseContext(parent, ModelGrammar.Options.DEFAULT, scope);
        assertSame(parent.parsers(), local.parsers());
        assertSame(parent.lookups(), local.lookups());
        assertSame(parent.identifierQuoting(), local.identifierQuoting());
        assertSame(parent.capabilities(), local.capabilities());
        assertSame(parent.operatorPolicy(), local.operatorPolicy());
        assertSame(parent.callstack(), local.callstack());
        assertTrue(local.parse(Query.class, "SELECT sales[1] FROM t").isError());
        assertTrue(local.parse(QueryExpr.class, "(SELECT sales[1] FROM t)").isError());
        assertTrue(parent.callstack().isEmpty());
    }

    @Test
    void postfixRejectsNonMeasuresAndDetachedParserReadsCells() {
        var ctx = ParseContext.of(new OracleSpecs());
        var parser = new CellRefExprParser();
        var cursor = Cursor.of("[1]", ctx.identifierQuoting());
        assertTrue(parser.parse(lit(1), cursor, ctx).isError());
        var denied = ParseContext.of(new AnsiSpecs());
        assertThrows(ParserException.class,
            () -> parser.parse(col("sales"), Cursor.of("[1]", denied.identifierQuoting()), denied));
        assertEquals(id("baseline"), ctx.parse(CellRefExpr.class, "baseline.sales[1]").value().model());
        assertTrue(new ModelAggregateExprParser().parse(col("sales"), Cursor.of("[1]", ctx.identifierQuoting()), ctx).isError());
    }

    @Test
    void namedScopeSeparatesMainAndReferenceDeclarationsAndRejectsConflicts() {
        var reference = referenceModel("rates", select(col("currency"), col("rate")).from(tbl("rates")).build())
            .dimension("currency").measure("rate").build();
        var scope = ModelParseScope.of(id("forecast"), List.of(modelColumn("y")),
            List.of(modelColumn("sales")), List.of(reference));
        assertTrue(scope.contains(id("forecast"), id("sales")));
        assertFalse(scope.contains(id("forecast"), id("rate")));
        assertTrue(scope.contains(id("rates"), id("rate")));
        assertFalse(scope.contains(id("rates"), id("sales")));
        assertFalse(scope.contains(id("missing"), id("sales")));
        var unnamed = ModelParseScope.of(null, List.of(modelColumn("y")), List.of(modelColumn("sales")), List.of(reference));
        assertTrue(unnamed.contains(id("rates"), id("rate")));
        assertEquals(1, scope.dimensionCount(null));
        assertEquals(1, scope.dimensionCount(id("forecast")));
        assertEquals(1, scope.dimensionCount(id("rates")));
        assertThrows(IllegalArgumentException.class, () -> ModelParseScope.of(null, List.of(), List.of(), List.of(reference, reference)));
        assertThrows(IllegalArgumentException.class, () -> ModelParseScope.of(id("RATES"), List.of(), List.of(), List.of(reference)));
        assertThrows(UnsupportedOperationException.class, () -> scope.references().clear());
        var ctx = ParseContext.of(new OracleSpecs());
        var local = new ModelParseContext(ctx, ModelGrammar.Options.DEFAULT, scope);
        assertTrue(local.parse(CellFor.class, "FOR missing IN (1)").isError());
        assertTrue(local.parse(MainModel.class, "DIMENSION BY (y) MEASURES (sales) (sales[1] = 1)").ok());
        var withoutScope = new ModelParseContext(ctx, ModelGrammar.Options.DEFAULT, null);
        assertTrue(withoutScope.parse(MainModel.class, "DIMENSION BY (y) MEASURES (sales) (sales[1] = 1)").ok());
        assertTrue(withoutScope.parse(CellRefExpr.class, "sales[1]").ok());
    }

    @Test
    void modelExpressionFramesStopAtNestedQueryBoundaries() {
        var ctx = ParseContext.of(new OracleSpecs());
        assertFalse(ModelGrammar.inRules(ctx));
        assertFalse(ModelGrammar.inRules(new ModelParseContext(ctx, ModelGrammar.Options.DEFAULT, null)));
        assertFalse(ModelGrammar.inRules(new ModelParseContext(ctx, ModelGrammar.Options.DEFAULT, ModelParseScope.of(List.of()))));
        assertTrue(ModelGrammar.inRules(new ModelParseContext(ctx, ModelGrammar.Options.DEFAULT, ModelParseScope.of(List.of(modelColumn("sales"))))));
        for (var type : List.of(ModelRules.class, ModelRule.class, IterationSpec.class, CellRefExpr.class,
            CellTarget.class, ModelAggregateExpr.class, CellSelector.Value.class, CellFor.Range.class,
            PreviousModelValueExpr.class, PresenceValueExpr.class, CellPresentPredicate.class, IsAnyPredicate.class)) {
            ctx.callstack().push(type);
            assertTrue(ModelGrammar.inRules(ctx), type.getSimpleName());
            for (var boundary : List.of(Query.class, SelectQuery.class, QueryExpr.class)) {
                ctx.callstack().push(boundary);
                assertFalse(ModelGrammar.inRules(ctx), boundary.getSimpleName());
                ctx.callstack().pop();
            }
            ctx.callstack().pop();
        }
    }
}
