package io.sqm.render.ansi;

import io.sqm.core.*;
import io.sqm.core.dialect.*;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;
import io.sqm.render.spi.RenderersRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class ModelRendererRegistrationTest {
    @Test
    void sharedQueryRendererDelegatesModelBetweenHavingAndOrderBy() {
        var clause = model().dimension("year_key").measure("sales")
            .rule(cellTarget("sales", 2026), lit(1)).build();
        var renderers = Renderers.ansiCopy().register(new Renderer<ModelClause>() {
            @Override
            public void render(ModelClause node, RenderContext ctx, SqlWriter w) {
                assertSame(clause, node);
                w.append("MODEL STUB");
            }

            @Override
            public Class<ModelClause> targetType() {
                return ModelClause.class;
            }
        });
        var ctx = RenderContext.of(new AnsiDialect() {
            @Override
            public DialectCapabilities capabilities() {
                return VersionedDialectCapabilities.builder(SqlDialectVersion.of(1, 0))
                    .supports(SqlFeature.MODEL_CLAUSE).build();
            }

            @Override
            public RenderersRepository renderers() {
                return renderers;
            }
        });
        var query = select(col("sales")).having(col("sales").gt(0))
            .model(clause).orderBy(col("sales")).build();

        assertEquals("SELECT sales\nHAVING sales > 0\nMODEL STUB\nORDER BY sales", ctx.render(query).sql());
    }

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
    void allNodesHaveExplicitRegisteredRejectingRenderers() {
        var incapable = RenderContext.of(new AnsiDialect());
        var enabled = RenderContext.of(new AnsiDialect() {
            @Override
            public DialectCapabilities capabilities() {
                return VersionedDialectCapabilities.builder(SqlDialectVersion.of(1, 0))
                    .supports(SqlFeature.MODEL_CLAUSE).build();
            }
        });
        for (var entry : samples().entrySet()) {
            assertNotNull(Renderers.ansiCopy().require(entry.getKey()));
            reject(entry.getKey(), entry.getValue(), incapable);
            reject(entry.getKey(), entry.getValue(), enabled);
        }
    }

    private <T extends Node> void reject(Class<T> type, Node node, RenderContext ctx) {
        var writer = new DefaultSqlWriter(ctx);
        var error = assertThrows(UnsupportedDialectFeatureException.class,
            () -> Renderers.ansi().require(type).render(type.cast(node), ctx, writer));
        assertTrue(error.getMessage().contains("MODEL"), error::getMessage);
        assertEquals("", writer.toText(List.of()).sql());
    }
}
