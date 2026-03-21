package io.sqm.dbit.mysql;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MySqlLiveFeatureCoverageTest {
    @Test
    void all_mysql_live_features_have_execution_coverage() {
        Set<MySqlLiveFeature> expected = EnumSet.allOf(MySqlLiveFeature.class);
        assertEquals(expected, MySqlExecutionCases.coveredFeatures());
    }
}
