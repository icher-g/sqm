package io.sqm.dbit.oracle;

import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.parser.oracle.spi.OracleSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.render.oracle.spi.OracleDialect;
import io.sqm.render.spi.RenderContext;
import io.sqm.validate.oracle.OracleValidationDialect;
import io.sqm.validate.schema.SchemaStatementValidator;
import io.sqm.validate.schema.ValidationCatalogSchemas;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class OracleModelExecutionCasesTest {
    @Test
    void every_query_fixture_round_trips_and_passes_oracle_validation_without_docker() {
        var parser = ParseContext.of(new OracleSpecs());
        var renderer = RenderContext.of(new OracleDialect());
        var validator = SchemaStatementValidator.of(ValidationCatalogSchemas.allowEverything(), OracleValidationDialect.of());
        for (var sample : OracleModelExecutionCases.queries()) {
            var sql = renderer.render(sample.query()).sql();
            var parsed = parser.parse(Query.class, sql);
            assertTrue(parsed.ok(), () -> sample.id() + ": " + parsed.errorMessage() + "\n" + sql);
            assertEquals(sample.query(), parsed.value(), sample.id());
            var validation = validator.validate(sample.query());
            assertTrue(validation.ok(), () -> sample.id() + ": " + validation.problems());
            assertNotNull(assertInstanceOf(SelectQuery.class, sample.query()).orderBy(), sample.id());
            assertFalse(sample.expectedRows().isEmpty(), sample.id());
        }
    }

    @Test
    void the_dedicated_suite_covers_every_model_feature_with_unique_case_ids() {
        var expected = EnumSet.noneOf(OracleLiveFeature.class);
        for (var feature : OracleLiveFeature.values()) {
            if (feature.name().startsWith("MODEL_")) expected.add(feature);
        }
        var covered = EnumSet.noneOf(OracleLiveFeature.class);
        var ids = new HashSet<String>();
        for (var sample : OracleModelExecutionCases.cases()) {
            assertTrue(ids.add(sample.id()), sample.id());
            covered.addAll(sample.features());
        }
        assertEquals(expected, covered);
        assertEquals(41, ids.size());
    }
}
