package io.sqm.transpile.builtin;

import io.sqm.core.PatternRecognitionTable;
import io.sqm.core.Statement;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.transpile.RewriteFidelity;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileOptions;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class MatchRecognizeUnsupportedRuleTest {
    @Test
    void exposesGlobalRuntimeGuardShape() {
        var rule = new MatchRecognizeUnsupportedRule();

        assertEquals("match-recognize-unsupported", rule.id());
        assertEquals(Set.of(), rule.sourceDialects());
        assertEquals(Set.of(), rule.targetDialects());
    }

    @Test
    void leavesStatementsWithoutPatternRecognitionUnchanged() {
        var statement = select(col("id")).from(tbl("sales")).build();
        var result = new MatchRecognizeUnsupportedRule()
            .apply(statement, context(SqlDialectId.ORACLE, SqlDialectId.POSTGRESQL));

        assertEquals(RewriteFidelity.EXACT, result.fidelity());
        assertFalse(result.changed());
        assertTrue(result.problems().isEmpty());
    }

    @Test
    void preservesOracleNativeRelationsExactly() {
        var statement = select(star()).from(patternRelation()).build();
        var result = new MatchRecognizeUnsupportedRule()
            .apply(statement, context(SqlDialectId.ORACLE, SqlDialectId.ORACLE));

        assertEquals(RewriteFidelity.EXACT, result.fidelity());
        assertFalse(result.changed());
        assertTrue(result.problems().isEmpty());
    }

    @Test
    void rejectsUnsupportedTargetWithStableTopLevelRelationPath() {
        var statement = select(star()).from(patternRelation()).build();
        var result = new MatchRecognizeUnsupportedRule()
            .apply(statement, context(SqlDialectId.ORACLE, SqlDialectId.POSTGRESQL));

        assertEquals(RewriteFidelity.UNSUPPORTED, result.fidelity());
        var problem = result.problems().getFirst();
        assertEquals("UNSUPPORTED_MATCH_RECOGNIZE", problem.code());
        assertEquals("select.matchRecognize[0]", problem.clausePath());
        assertTrue(problem.message().contains(problem.clausePath()));
    }

    @Test
    void detectsNestedQueryJoinCteAndInsertSourceLocations() {
        var nested = select(star())
            .from(tbl(select(star()).from(patternRelation()).build()).as("nested"))
            .build();
        var joined = select(star())
            .from(tbl("lookup").as("l"))
            .join(inner(patternRelation()).on(col("l", "id").eq(col("mr", "id"))))
            .build();
        var cteQuery = with(cte("matches", select(star()).from(patternRelation()).build()))
            .body(select(star()).from(tbl("matches")).build());
        var insert = insert(tbl("match_archive"))
            .columns(id("id"))
            .query(select(col("id")).from(patternRelation()).build())
            .build();

        assertPath(nested, "select.queryTable.select.matchRecognize[0]");
        assertPath(joined, "select.join.matchRecognize[0]");
        assertPath(cteQuery, "with.cte.select.matchRecognize[0]");
        assertPath(insert, "insert.select.matchRecognize[0]");
    }

    @Test
    void customExactCompatibilityHookCanEnableFutureDialectPair() {
        var future = SqlDialectId.of("future-pattern-sql");
        var rule = new MatchRecognizeUnsupportedRule((table, context) ->
            context.sourceDialect().equals(SqlDialectId.ORACLE)
                && context.targetDialect().equals(future)
                && table.subsets().isEmpty());
        var result = rule.apply(
            select(star()).from(patternRelation()).build(),
            context(SqlDialectId.ORACLE, future)
        );

        assertEquals(RewriteFidelity.EXACT, result.fidelity());
        assertTrue(result.problems().isEmpty());
    }

    private static void assertPath(Statement statement, String expectedPath) {
        var result = new MatchRecognizeUnsupportedRule()
            .apply(statement, context(SqlDialectId.ORACLE, SqlDialectId.MYSQL));

        assertEquals(expectedPath, result.problems().getFirst().clausePath());
    }

    private static PatternRecognitionTable patternRelation() {
        return matchRecognize(tbl("sales"))
            .measure(matchNumber(), "match_no")
            .pattern(patternVar("A"))
            .define("A", patternColumn("A", "amount").gt(0))
            .as("mr")
            .build();
    }

    private static TranspileContext context(SqlDialectId source, SqlDialectId target) {
        return new TranspileContext(source, target, TranspileOptions.defaults(), Optional.empty(), Optional.empty());
    }
}
