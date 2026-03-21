package io.sqm.dbit.postgresql;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PostgresLiveFeatureCoverageTest {
    @Test
    void all_postgres_live_features_have_execution_coverage() {
        Set<PostgresLiveFeature> expected = EnumSet.allOf(PostgresLiveFeature.class);
        assertEquals(expected, PostgresExecutionCases.coveredFeatures());
    }
}
