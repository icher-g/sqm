package io.sqm.parser.ansi;

import io.sqm.core.*;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.VersionedDialectCapabilities;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelParserRegistrationTest {
    @Test
    void everyModelNodeHasADedicatedAnsiParser() {
        var ansi = ParseContext.of(new AnsiSpecs());
        var enabled = ParseContext.of(new AnsiSpecs() {
            @Override
            public DialectCapabilities capabilities() {
                return VersionedDialectCapabilities.builder(SqlDialectVersion.of(1, 0))
                    .supports(SqlFeature.MODEL_CLAUSE).build();
            }
        });
        for (Class<? extends Node> type : List.of(
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
            IsAnyPredicate.class)) {
            assertNotNull(Parsers.ansiCopy().require(type));
            for (var ctx : List.of(ansi, enabled)) {
                var parsed = ctx.parse(type, "MODEL");
                assertTrue(parsed.isError(), type.getSimpleName());
                assertTrue(parsed.errorMessage().contains("MODEL"), parsed::errorMessage);
            }
        }
    }
}
