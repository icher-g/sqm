package io.sqm.dbit.mysql;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.stream.Stream;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

@Testcontainers(disabledWithoutDocker = true)
class MySqlDslExecutionIT extends MySqlExecutionHarness {
    @BeforeEach
    void setUpSchema() throws Exception {
        resetDslSchema();
    }

    @TestFactory
    Stream<DynamicTest> executes_mysql_dsl_cases_against_live_db() {
        return MySqlExecutionCases.cases().stream()
            .map(testCase -> dynamicTest(testCase.id(), () -> testCase.execution().run(this)));
    }
}
