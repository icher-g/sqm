package io.sqm.dbit.oracle;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OraclePatternRecognitionExecutionCasesTest {
    @Test
    void declares_the_complete_story_case_matrix_with_unique_ids() {
        var cases = OraclePatternRecognitionExecutionCases.cases();
        var ids = cases.stream().map(testCase -> testCase.id()).toList();

        assertEquals(15, cases.size());
        assertEquals(ids.size(), new HashSet<>(ids).size());
    }
}
