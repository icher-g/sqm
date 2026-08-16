package io.sqm.dbit.oracle;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OracleLiveFeatureCoverageTest {
    @Test
    void all_oracle_live_features_have_execution_coverage() {
        Set<OracleLiveFeature> expected = EnumSet.allOf(OracleLiveFeature.class);
        assertEquals(expected, OracleExecutionCases.coveredFeatures());
    }
}
