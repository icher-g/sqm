package io.sqm.parser.oracle;

import io.sqm.core.*;
import io.sqm.parser.ansi.AnsiSpecs;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.oracle.spi.OracleSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.sqm.dsl.Dsl.*;
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
        Map.entry(CellSelector.Value.class, "1"));

    @Test
    void everyNodeHasAnExplicitAnsiAndOracleBoundary() {
        var ansi = ParseContext.of(new AnsiSpecs());
        var oracle = ParseContext.of(new OracleSpecs());
        for (var type : TYPES) {
            assertNotNull(io.sqm.parser.ansi.Parsers.ansiCopy().require(type));
            assertNotNull(Parsers.oracleCopy().require(type));
            var result = ansi.parse(type, "anything");
            assertTrue(result.isError(), type.getSimpleName());
            assertTrue(Objects.requireNonNull(result.errorMessage()).contains("MODEL"), result::errorMessage);
            if (!BASIC.containsKey(type)) {
                var unsupported = oracle.parse(type, "anything");
                assertTrue(unsupported.isError());
                assertTrue(Objects.requireNonNull(unsupported.errorMessage()).contains("#506"), unsupported::errorMessage);
            }
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
        assertThrows(io.sqm.parser.core.ParserException.class,
            () -> parser.parse(col("sales"), Cursor.of("[1]", denied.identifierQuoting()), denied));
        assertTrue(Objects.requireNonNull(ctx.parse(CellRefExpr.class, "baseline.sales[1]").errorMessage()).contains("#506"));
    }
}
