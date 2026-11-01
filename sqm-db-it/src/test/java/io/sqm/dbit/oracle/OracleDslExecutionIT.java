package io.sqm.dbit.oracle;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.stream.Stream;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
class OracleDslExecutionIT extends OracleExecutionHarness {
    @Test
    void flashback_reads_committed_rows_immediately_after_fixture_creation() throws Exception {
        var query = select(col("id")).from(tbl("flashback_users").withVersion(asOfTimestamp(param("as_of"))))
            .orderBy(col("id")).build();
        // Recreate fresh DDL repeatedly so the check cannot rely on earlier cases aging the table.
        for (int fixture = 0; fixture < 3; fixture++) {
            resetFlashbackFixture();
            assertEquals(List.of("1", "2"), queryRows(render(query), List.of(currentDatabaseTimestamp())));
        }
    }

    @TestFactory
    Stream<DynamicTest> executes_oracle_dsl_cases_against_live_db() {
        return OracleExecutionCases.cases().stream()
            .map(testCase -> dynamicTest(testCase.id(), () -> {
                resetDslSchema();
                testCase.execution().run(this);
            }));
    }

    @TestFactory
    Stream<DynamicTest> executes_oracle_model_cases_against_live_db() {
        return OracleModelExecutionCases.cases().stream()
            .map(testCase -> dynamicTest(testCase.id(), () -> {
                System.out.println("[Oracle MODEL] " + testCase.id());
                resetModelSchema();
                testCase.execution().run(this);
            }));
    }
}
