package io.sqm.dbit.oracle;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.stream.Stream;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

@Testcontainers
class OracleModelExecutionIT extends OracleExecutionHarness {
    @TestFactory
    Stream<DynamicTest> executes_model_cases_against_live_oracle() {
        return OracleModelExecutionCases.cases().stream()
            .map(testCase -> dynamicTest(testCase.id(), () -> {
                System.out.println("[Oracle MODEL] " + testCase.id());
                resetModelSchema();
                testCase.execution().run(this);
            }));
    }

    private void resetModelSchema() throws Exception {
        for (var table : new String[]{"model_sales", "model_rates", "model_years", "model_targets", "model_output"}) {
            dropTableIfExists(table);
        }
        // Each setup connection uses JDBC autocommit; every test starts from committed rows.
        executeStatements(
            "create table model_sales (region varchar2(1), product varchar2(10), year_key number(4), amount number(19))",
            "create table model_rates (product varchar2(10) primary key, rate number(19))",
            "create table model_years (year_key number(4) primary key)",
            "create table model_targets (product varchar2(10), year_key number(4))",
            "create table model_output (product varchar2(10), year_key number(4), sales number(19))",
            "insert into model_sales values ('N', 'Bike', 2025, 10)",
            "insert into model_sales values ('N', 'Bike', 2026, 20)",
            "insert into model_sales values ('N', 'Car', 2025, 30)",
            "insert into model_sales values ('N', 'Car', 2026, null)",
            "insert into model_sales values ('S', 'Bike', 2025, 100)",
            "insert into model_sales values ('S', 'Bike', 2026, 200)",
            "insert into model_sales values ('S', 'Car', 2025, 300)",
            "insert into model_sales values ('S', 'Car', 2026, null)",
            "insert into model_rates values ('Bike', 2)",
            "insert into model_rates values ('Car', 3)",
            "insert into model_years values (2027)",
            "insert into model_years values (2028)",
            "insert into model_targets values ('Bike', 2027)",
            "insert into model_targets values ('Car', 2028)"
        );
    }
}
