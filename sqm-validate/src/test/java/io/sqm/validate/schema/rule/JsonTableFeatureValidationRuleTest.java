package io.sqm.validate.schema.rule;

import io.sqm.catalog.model.CatalogSchema;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.VersionedDialectCapabilities;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class JsonTableFeatureValidationRuleTest {
    @Test
    void reportsUnsupportedJsonTableFeature() {
        var context = new SchemaValidationContext(CatalogSchema.of(List.of()));
        var table = jsonTable(col("payload"), jsonPath("$"), jsonScalar("id", type("NUMBER"), jsonPath("$.id")));

        new JsonTableFeatureValidationRule(
            "TestSQL",
            SqlDialectVersion.of(1),
            VersionedDialectCapabilities.builder(SqlDialectVersion.of(1)).build()
        ).validate(table, context);

        assertEquals(1, context.problems().size());
        assertTrue(context.problems().getFirst().message().contains(SqlFeature.JSON_TABLE.description()));
    }

    @Test
    void acceptsSupportedJsonTableFeature() {
        var context = new SchemaValidationContext(CatalogSchema.of(List.of()));
        var table = jsonTable(col("payload"), jsonPath("$"), jsonScalar("id", type("NUMBER"), jsonPath("$.id")));

        new JsonTableFeatureValidationRule(
            "TestSQL",
            SqlDialectVersion.of(1),
            VersionedDialectCapabilities.builder(SqlDialectVersion.of(1)).supports(SqlFeature.JSON_TABLE).build()
        ).validate(table, context);

        assertTrue(context.problems().isEmpty());
    }
}
