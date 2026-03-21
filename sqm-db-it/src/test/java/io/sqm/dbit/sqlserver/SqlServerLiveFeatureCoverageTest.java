package io.sqm.dbit.sqlserver;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SqlServerLiveFeatureCoverageTest {
    @Test
    void all_sqlserver_live_features_have_execution_coverage() {
        Set<SqlServerLiveFeature> expected = EnumSet.allOf(SqlServerLiveFeature.class);
        assertEquals(expected, SqlServerExecutionCases.coveredFeatures());
    }
}
