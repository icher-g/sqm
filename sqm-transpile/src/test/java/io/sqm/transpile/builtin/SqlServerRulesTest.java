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

    @Test
    void sqlServerHintRuleDropsSqlServerHintsForNonSqlServerTargets() {
        Statement statement = Dsl.select(Dsl.col("id"))
            .from(Dsl.tbl("users").withNoLock())
            .build();

        var result = new SqlServerHintDroppingRule().apply(statement, context(SqlDialectId.SQLSERVER, SqlDialectId.POSTGRESQL));

        assertEquals(io.sqm.transpile.RewriteFidelity.APPROXIMATE, result.fidelity());
        assertTrue(result.problems().isEmpty());
        assertEquals("SQLSERVER_HINTS_DROPPED", result.warnings().getFirst().code());
        assertTrue(((io.sqm.core.Table) ((io.sqm.core.SelectQuery) result.statement()).from()).hints().isEmpty());
    }

    @Test
    void sqlServerHintRuleDropsStatementAndGenericTableHintsToo() {
        Statement statement = Dsl.select(Dsl.col("id"))
            .from(Dsl.tbl("users").hint("INDEX", "idx_users_name"))
            .hint("QUERYTRACEON", 4199)
            .build();

        var result = new SqlServerHintDroppingRule().apply(statement, context(SqlDialectId.SQLSERVER, SqlDialectId.POSTGRESQL));

        assertEquals(io.sqm.transpile.RewriteFidelity.APPROXIMATE, result.fidelity());
        assertTrue(result.problems().isEmpty());
        assertEquals("SQLSERVER_HINTS_DROPPED", result.warnings().getFirst().code());
        assertTrue(result.statement().hints().isEmpty());
        assertTrue(((io.sqm.core.Table) ((io.sqm.core.SelectQuery) result.statement()).from()).hints().isEmpty());
    }

    @Test
    void sqlServerOutputRuleRejectsSqlServerOutputForNonSqlServerTargets() {
        Statement statement = Dsl.update(Dsl.tbl("users"))
            .set(Dsl.set("name", Dsl.lit("alice")))
            .result(Dsl.deleted("name"), Dsl.inserted("name"))
            .build();

        var result = new SqlServerOutputUnsupportedRule().apply(statement, context(SqlDialectId.SQLSERVER, SqlDialectId.POSTGRESQL));

        assertEquals(io.sqm.transpile.RewriteFidelity.UNSUPPORTED, result.fidelity());
        assertFalse(result.problems().isEmpty());
        assertEquals("UNSUPPORTED_SQLSERVER_OUTPUT", result.problems().getFirst().code());
    }

    @Test
    void sqlServerOutputRuleIgnoresGenericReturningStyleResultClauses() {
        Statement statement = Dsl.update(Dsl.tbl("users"))
            .set(Dsl.set("name", Dsl.lit("alice")))
            .result(Dsl.col("id").toSelectItem())
            .build();

        var result = new SqlServerOutputUnsupportedRule().apply(statement, context(SqlDialectId.SQLSERVER, SqlDialectId.POSTGRESQL));

        assertFalse(result.changed());
        assertSame(statement, result.statement());
        assertEquals("No SQL Server-specific OUTPUT usage detected", result.description());
    }

    @Test
    void sqlServerMergeRuleRejectsMergeForNonSqlServerTargets() {
        Statement statement = Dsl.merge("users")
            .source(Dsl.tbl("src").as("s"))
            .on(Dsl.col("users", "id").eq(Dsl.col("s", "id")))
            .top(Dsl.topPercent(Dsl.lit(10)))
            .whenMatchedDelete()
            .whenNotMatchedBySourceDelete(Dsl.col("users", "archived").eq(Dsl.lit(true)))
            .result(Dsl.deleted("id"), Dsl.inserted("id"))
            .build();

        var result = new SqlServerMergeUnsupportedRule().apply(statement, context(SqlDialectId.SQLSERVER, SqlDialectId.POSTGRESQL));

        assertEquals(io.sqm.transpile.RewriteFidelity.UNSUPPORTED, result.fidelity());
        assertFalse(result.problems().isEmpty());
        assertEquals("UNSUPPORTED_SQLSERVER_MERGE", result.problems().getFirst().code());
    }
}
