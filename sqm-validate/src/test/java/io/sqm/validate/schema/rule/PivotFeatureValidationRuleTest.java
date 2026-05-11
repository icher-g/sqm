package io.sqm.validate.schema.rule;

import io.sqm.core.TableRef;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.VersionedDialectCapabilities;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.ValidationCatalogSchemas;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PivotFeatureValidationRuleTest {
    private static final SqlDialectVersion VERSION = SqlDialectVersion.of(1);

    @Test
    void exposesTableRefNodeType() {
        assertEquals(TableRef.class, supportedRule().nodeType());
    }

    @Test
    void reportsUnsupportedPivotAndUnpivotFeatures() {
        var context = context();
        var rule = unsupportedRule();

        rule.validate(pivotTable(), context);
        rule.validate(unpivotTable(), context);

        assertEquals(2, context.problems().size());
        assertEquals(ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED, context.problems().getFirst().code());
        assertEquals("from.pivot", context.problems().getFirst().clausePath());
        assertEquals("from.unpivot", context.problems().get(1).clausePath());
    }

    @Test
    void acceptsSupportedPivotAndUnpivotFeaturesAndIgnoresOtherTableRefs() {
        var context = context();
        var rule = supportedRule();

        rule.validate(pivotTable(), context);
        rule.validate(unpivotTable(), context);
        rule.validate(tbl("sales"), context);

        assertTrue(context.problems().isEmpty());
    }

    private static PivotFeatureValidationRule unsupportedRule() {
        return new PivotFeatureValidationRule(
            "test",
            VERSION,
            VersionedDialectCapabilities.builder(VERSION).build()
        );
    }

    private static PivotFeatureValidationRule supportedRule() {
        return new PivotFeatureValidationRule(
            "test",
            VERSION,
            VersionedDialectCapabilities.builder(VERSION)
                .supports(SqlFeature.PIVOT_TABLE)
                .supports(SqlFeature.UNPIVOT_TABLE)
                .build()
        );
    }

    private static io.sqm.core.PivotTable pivotTable() {
        return pivot(
            tbl("sales"),
            List.of(pivotMeasure(func("sum", col("amount")))),
            col("quarter"),
            List.of(pivotValue(lit("Q1")))
        );
    }

    private static io.sqm.core.UnpivotTable unpivotTable() {
        return unpivot(
            tbl("sales"),
            "amount",
            "quarter",
            List.of(unpivotInput("q1", lit("Q1")))
        );
    }

    private static SchemaValidationContext context() {
        return new SchemaValidationContext(ValidationCatalogSchemas.allowEverything());
    }
}
