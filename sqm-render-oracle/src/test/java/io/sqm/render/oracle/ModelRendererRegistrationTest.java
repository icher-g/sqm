package io.sqm.render.oracle;

import io.sqm.core.*;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.oracle.spi.OracleDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class ModelRendererRegistrationTest {
    private static final Set<Class<? extends Node>> BASIC = Set.of(
        ModelClause.class, MainModel.class, ModelColumn.class, ModelRules.class, ModelRule.class, CellTarget.class, CellRefExpr.class, CellAddress.class, CellSelector.class, CellSelector.Value.class);

    private Map<Class<? extends Node>, Node> samples() {
        var cell = cellRef("sales", 2025);
        var target = cellTarget("sales", 2026);
        return Map.ofEntries(
            Map.entry(ModelClause.class, model().dimension("year_key").measure("sales").rule(target, lit(1)).build()),
            Map.entry(MainModel.class, mainModel().dimension("year_key").measure("sales").rule(target, lit(1)).build()),
            Map.entry(ReferenceModel.class, referenceModel("baseline", select(lit(1)).build()).dimension("year_key").measure("sales").build()),
            Map.entry(ModelColumn.class, modelColumn("sales")),
            Map.entry(ModelRules.class, modelRules().rule(target, lit(1)).build()),
            Map.entry(ModelRule.class, modelRule(target, lit(1))),
            Map.entry(IterationSpec.class, iterationSpec(lit(2))),
            Map.entry(CellTarget.class, target),
            Map.entry(CellRefExpr.class, cell),
            Map.entry(CellAddress.class, cellValue(2026)),
            Map.entry(CellSelector.class, cellValue(2026)),
            Map.entry(CellSelector.Value.class, cellValue(2026)),
            Map.entry(CellSelector.Condition.class, cellCondition(col("year_key").eq(2026))),
            Map.entry(CellFor.class, cellForValues("year_key", 2025, 2026)),
            Map.entry(CellFor.Values.class, cellForValues("year_key", 2025, 2026)),
            Map.entry(CellFor.Range.class, cellForRange("year_key", 2025, 2027, RangeDirection.INCREMENT, 1)),
            Map.entry(ModelAggregateExpr.class, modelAggregateExpr(func("SUM", col("sales")), List.of(cellAny()))),
            Map.entry(CurrentDimensionExpr.class, currentDimension()),
            Map.entry(IterationNumberExpr.class, iterationNumber()),
            Map.entry(PreviousModelValueExpr.class, previousModelValueExpr(cell)),
            Map.entry(PresenceValueExpr.class, presenceValue(cell, lit(1), lit(0))),
            Map.entry(CellPresentPredicate.class, cellIsPresent(cell)),
            Map.entry(IsAnyPredicate.class, isAny()));
    }

    @Test
    void everyDirectOracleNodeRendererHonorsItsCapabilityBoundary() {
        var incapable = RenderContext.of(new AnsiDialect());
        var oracle = RenderContext.of(new OracleDialect());
        for (var entry : samples().entrySet()) {
            assertNotNull(Renderers.oracleCopy().require(entry.getKey()));
            render(entry.getKey(), entry.getValue(), incapable, true);
            render(entry.getKey(), entry.getValue(), oracle, !BASIC.contains(entry.getKey()));
        }
    }

    @Test
    void familyRoutersDelegateToEveryChildVariant() {
        var ctx = RenderContext.of(new OracleDialect());
        render(CellAddress.class, cellForValues("year_key", 2026), ctx, true);
        render(CellSelector.class, cellAny(), ctx, true);
    }

    private <T extends Node> void render(Class<T> type, Node node, RenderContext ctx, boolean rejected) {
        var renderer = Renderers.oracle().require(type);
        var writer = new DefaultSqlWriter(ctx);
        if (rejected) {
            var error = assertThrows(UnsupportedDialectFeatureException.class,
                () -> renderer.render(type.cast(node), ctx, writer), type.getSimpleName());
            assertTrue(error.getMessage().contains("MODEL"), error::getMessage);
            assertEquals("", writer.toText(List.of()).sql());
        }
        else {
            renderer.render(type.cast(node), ctx, writer);
            assertFalse(writer.toText(List.of()).sql().isBlank(), type.getSimpleName());
        }
    }
}
