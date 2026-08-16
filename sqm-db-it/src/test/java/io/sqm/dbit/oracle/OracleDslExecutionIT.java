package io.sqm.dbit.oracle;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.stream.Stream;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

@Testcontainers(disabledWithoutDocker = true)
class OracleDslExecutionIT extends OracleExecutionHarness {
    @TestFactory
    Stream<DynamicTest> executes_oracle_dsl_cases_against_live_db() {
        return OracleExecutionCases.cases().stream()
            .map(testCase -> dynamicTest(testCase.id(), () -> {
                resetDslSchema();
                testCase.execution().run(this);
            }));
    }
}
