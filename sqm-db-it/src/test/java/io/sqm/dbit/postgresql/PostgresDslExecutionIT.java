package io.sqm.dbit.postgresql;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.stream.Stream;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

@Testcontainers(disabledWithoutDocker = true)
class PostgresDslExecutionIT extends PostgresExecutionHarness {
    @BeforeEach
    void setUpSchema() throws Exception {
        resetDslSchema();
    }

    @TestFactory
    Stream<DynamicTest> executes_postgres_dsl_cases_against_live_db() {
        return PostgresExecutionCases.cases().stream()
            .map(testCase -> dynamicTest(testCase.id(), () -> testCase.execution().run(this)));
    }
}
