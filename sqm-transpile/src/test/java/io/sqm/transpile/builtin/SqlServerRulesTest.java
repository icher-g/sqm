package io.sqm.transpile.builtin;

import io.sqm.core.Statement;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.dsl.Dsl;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlServerRulesTest {

    private static TranspileContext context(SqlDialectId source, SqlDialectId target) {
        return new TranspileContext(source, target, TranspileOptions.defaults(), java.util.Optional.empty(), java.util.Optional.empty());
    }

    @Test
    void standardLimitToSqlServerTopLeavesOffsetQueriesUnchanged() {
        Statement statement = Dsl.select(Dsl.col("id"))
            .from(Dsl.tbl("users"))
            .orderBy(Dsl.order(Dsl.col("id")))
            .limit(5)
            .offset(2)
            .build();

        var result = new StandardLimitToSqlServerTopRule().apply(statement, context(SqlDialectId.ANSI, SqlDialectId.SQLSERVER));

        assertFalse(result.changed());
        assertSame(statement, result.statement());
        assertEquals("No limit-only SELECT usage detected", result.description());
    }

    @Test
    void sqlServerTopToLimitLeavesStatementsWithoutTopUnchanged() {
        Statement statement = Dsl.select(Dsl.col("id"))
            .from(Dsl.tbl("users"))
            .orderBy(Dsl.order(Dsl.col("id")))
            .limit(5)
            .offset(2)
            .build();

        var result = new SqlServerTopToLimitRule().apply(statement, context(SqlDialectId.SQLSERVER, SqlDialectId.POSTGRESQL));

        assertFalse(result.changed());
        assertSame(statement, result.statement());
        assertEquals("No SQL Server TOP usage detected", result.description());
    }

    @Test
    void sqlServerTopToLimitRejectsTopPercent() {
        Statement statement = Dsl.select(Dsl.col("id"))
            .from(Dsl.tbl("users"))
            .top(Dsl.topPercent(Dsl.lit(5)))
            .build();

        var result = new SqlServerTopToLimitRule().apply(statement, context(SqlDialectId.SQLSERVER, SqlDialectId.POSTGRESQL));

        assertEquals(io.sqm.transpile.RewriteFidelity.UNSUPPORTED, result.fidelity());
        assertFalse(result.problems().isEmpty());
        assertEquals("UNSUPPORTED_SQLSERVER_TOP_PERCENT", result.problems().getFirst().code());
    }

    @Test
    void sqlServerTopToLimitRejectsTopWithTies() {
        Statement statement = Dsl.select(Dsl.col("id"))
            .from(Dsl.tbl("users"))
            .orderBy(Dsl.order(Dsl.col("id")))
            .top(Dsl.topWithTies(Dsl.lit(5)))
            .build();

        var result = new SqlServerTopToLimitRule().apply(statement, context(SqlDialectId.SQLSERVER, SqlDialectId.POSTGRESQL));

        assertEquals(io.sqm.transpile.RewriteFidelity.UNSUPPORTED, result.fidelity());
        assertFalse(result.problems().isEmpty());
        assertEquals("UNSUPPORTED_SQLSERVER_TOP_WITH_TIES", result.problems().getFirst().code());
    }

    @Test
    void postgresToSqlServerDistinctOnRuleLeavesBaselineStatementsUnchanged() {
        Statement statement = Dsl.select(Dsl.col("id")).from(Dsl.tbl("users")).build();

        var result = new PostgresToSqlServerDistinctOnUnsupportedRule()
            .apply(statement, context(SqlDialectId.POSTGRESQL, SqlDialectId.SQLSERVER));

        assertFalse(result.changed());
        assertSame(statement, result.statement());
        assertEquals("No DISTINCT ON usage detected", result.description());
    }
}
