package io.sqm.transpile.builtin;

import io.sqm.core.IsDistinctFromPredicate;
import io.sqm.core.NotPredicate;
import io.sqm.core.Statement;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.dsl.Dsl;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileOptions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MySqlToPostgresNullSafeComparisonRuleTest {
    private static final TranspileContext CONTEXT = new TranspileContext(
        SqlDialectId.of("mysql"),
        SqlDialectId.of("postgresql"),
        TranspileOptions.defaults(),
        Optional.empty(),
        Optional.empty()
    );

    @Test
    void rewritesPlainAndNegatedNullSafeComparisons() {
        Statement plain = Dsl.select(Dsl.col("id"))
            .from(Dsl.tbl("users"))
            .where(Dsl.col("first_name").nullSafeEq(Dsl.col("last_name")))
            .build();
        Statement negated = Dsl.select(Dsl.col("id"))
            .from(Dsl.tbl("users"))
            .where(NotPredicate.of(Dsl.col("first_name").nullSafeEq(Dsl.col("last_name"))))
            .build();

        var plainResult = new MySqlToPostgresNullSafeComparisonRule().apply(plain, CONTEXT);
        var negatedResult = new MySqlToPostgresNullSafeComparisonRule().apply(negated, CONTEXT);

        assertTrue(plainResult.changed());
        assertTrue(negatedResult.changed());
        assertInstanceOf(IsDistinctFromPredicate.class, ((io.sqm.core.SelectQuery) plainResult.statement()).where());
        assertInstanceOf(IsDistinctFromPredicate.class, ((io.sqm.core.SelectQuery) negatedResult.statement()).where());
        assertTrue(((IsDistinctFromPredicate) ((io.sqm.core.SelectQuery) plainResult.statement()).where()).negated());
        assertFalse(((IsDistinctFromPredicate) ((io.sqm.core.SelectQuery) negatedResult.statement()).where()).negated());
    }

    @Test
    void leavesRegularComparisonsUnchanged() {
        var statement = Dsl.select(Dsl.col("id"))
            .from(Dsl.tbl("users"))
            .where(Dsl.col("first_name").eq(Dsl.col("last_name")))
            .build();

        var result = new MySqlToPostgresNullSafeComparisonRule().apply(statement, CONTEXT);

        assertFalse(result.changed());
        assertSame(statement, result.statement());
    }

    @Test
    void traversesNestedPredicatesWithoutRewritingUnrelatedNotExpressions() {
        var statement = Dsl.select(Dsl.col("id"))
            .from(Dsl.tbl("users"))
            .where(NotPredicate.of(Dsl.col("first_name").eq(Dsl.col("last_name"))))
            .build();

        var result = new MySqlToPostgresNullSafeComparisonRule().apply(statement, CONTEXT);

        assertFalse(result.changed());
        assertSame(statement, result.statement());
    }

    @Test
    void exposesRuleMetadataForRegistryFiltering() {
        var rule = new MySqlToPostgresNullSafeComparisonRule();

        assertEquals("mysql-to-postgres-null-safe-comparison", rule.id());
        assertTrue(rule.sourceDialects().contains(SqlDialectId.of("mysql")));
        assertTrue(rule.targetDialects().contains(SqlDialectId.of("postgresql")));
    }
}
