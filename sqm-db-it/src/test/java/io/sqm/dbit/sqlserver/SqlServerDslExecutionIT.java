package io.sqm.dbit.sqlserver;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.stream.Stream;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

@Testcontainers(disabledWithoutDocker = true)
class SqlServerDslExecutionIT extends SqlServerExecutionHarness {
    @TestFactory
    Stream<DynamicTest> executes_sqlserver_dsl_cases_against_live_db() {
        return SqlServerExecutionCases.cases().stream()
            .map(testCase -> dynamicTest(testCase.id(), () -> {
                resetDslSchema();
                testCase.execution().run(this);
            }));
    }
}
