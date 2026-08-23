package io.sqm.parser.ansi;

import io.sqm.core.*;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.core.dialect.SqlDialectVersion;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class PatternRecognitionTableParserTest {
    private final ParseContext enabled = ParseContext.of(new TestSpecs());

    @Test
    void rejectsMatchRecognizeWhenAnsiCapabilitiesDoNotSupportIt() {
        var sql2008 = ParseContext.of(new AnsiSpecs(SqlDialectVersion.of(2008)));
        var result = sql2008.parse(Query.class,
            "SELECT * FROM sales MATCH_RECOGNIZE (PATTERN (A) DEFINE A AS amount > 0)");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("MATCH_RECOGNIZE is not supported"));
    }

    @Test
    void parsesCompleteClauseAndScopedExpressions() {
        var result = enabled.parse(Query.class, """
            SELECT *
            FROM sales
            MATCH_RECOGNIZE (
              PARTITION BY customer_id
              ORDER BY sale_date
              MEASURES MATCH_NUMBER() AS match_no,
                       CLASSIFIER() AS label,
                       FIRST(A.amount) AS first_amount,
                       FINAL LAST(B.amount, 1) AS last_amount
              ALL ROWS PER MATCH
              AFTER MATCH SKIP TO FIRST B
              PATTERN (^ A+? (B | C){2,5}? {- D+ -} $)
              SUBSET U = (B, C)
              DEFINE A AS A.amount > 0,
                     B AS B.amount > PREV(B.amount),
                     C AS C.amount > 0,
                     D AS D.amount > 0
            ) mr
            """);

        assertTrue(result.ok(), result.errorMessage());
        var query = assertInstanceOf(SelectQuery.class, result.value());
        var table = assertInstanceOf(PatternRecognitionTable.class, query.from());
        assertEquals("mr", table.alias().value());
        assertNotNull(table.partitionBy());
        assertNotNull(table.orderBy());
        assertEquals(4, table.measures().size());
        assertInstanceOf(MatchNumberExpr.class, table.measures().getFirst().expression());
        assertInstanceOf(ClassifierExpr.class, table.measures().get(1).expression());
        assertInstanceOf(PatternNavigationExpr.class, table.measures().get(2).expression());
        assertInstanceOf(PatternEvaluationExpr.class, table.measures().get(3).expression());
        assertEquals(RowsPerMatch.Mode.ALL, table.rowsPerMatch().mode());
        assertEquals(AfterMatchSkip.Position.FIRST, table.afterMatchSkip().position());
        assertInstanceOf(MatchPattern.Sequence.class, table.pattern());
        assertEquals(1, table.subsets().size());
        assertEquals(4, table.definitions().size());
        var comparison = assertInstanceOf(ComparisonPredicate.class, table.definitions().get(1).condition());
        assertInstanceOf(PatternColumnExpr.class, comparison.lhs());
        assertInstanceOf(PatternNavigationExpr.class, comparison.rhs());
    }

    @Test
    void normalizesOmittedSemanticDefaults() {
        var result = enabled.parse(Query.class,
            "SELECT * FROM sales MATCH_RECOGNIZE (PATTERN (A) DEFINE A AS amount > 0)");

        assertTrue(result.ok(), result.errorMessage());
        var table = assertInstanceOf(PatternRecognitionTable.class,
            assertInstanceOf(SelectQuery.class, result.value()).from());
        assertEquals(RowsPerMatch.Mode.ONE, table.rowsPerMatch().mode());
        assertEquals(AfterMatchSkip.Kind.PAST_LAST_ROW, table.afterMatchSkip().kind());
        assertInstanceOf(ColumnExpr.class,
            assertInstanceOf(ComparisonPredicate.class, table.definitions().getFirst().condition()).lhs());
    }

    @Test
    void parsesEveryPatternVariantDirectly() {
        assertTrue(enabled.parse(MatchPattern.Variable.class, "A").ok());
        assertTrue(enabled.parse(MatchPattern.Sequence.class, "A B").ok());
        assertTrue(enabled.parse(MatchPattern.Alternation.class, "A | B").ok());
        assertTrue(enabled.parse(MatchPattern.Permutation.class, "PERMUTE(A, B)").ok());
        assertTrue(enabled.parse(MatchPattern.Anchor.class, "^").ok());
        assertTrue(enabled.parse(MatchPattern.Empty.class, "()").ok());
        assertTrue(enabled.parse(MatchPattern.Exclusion.class, "{- A -}").ok());
        assertTrue(enabled.parse(MatchPattern.Quantified.class, "(A | B)+?").ok());
    }

    @Test
    void keepsPatternSpecificTextOrdinaryOutsidePatternScope() {
        assertInstanceOf(ColumnExpr.class, enabled.parse(Expression.class, "A.amount").value());
        assertInstanceOf(FunctionExpr.class, enabled.parse(Expression.class, "CLASSIFIER()").value());
    }

    @Test
    void exposesTransformParserMetadataAndStandaloneError() {
        var parser = new PatternRecognitionTableParser();
        assertEquals(PatternRecognitionTable.class, parser.targetType());
        assertTrue(parser.match(Cursor.of("MATCH_RECOGNIZE", enabled.identifierQuoting()), enabled));
        assertTrue(parser.parse(Cursor.of("MATCH_RECOGNIZE", enabled.identifierQuoting()), enabled).isError());
    }

    @Test
    void composesWithParenthesizedSourcesLaterTransformsAndJoins() {
        var parenthesized = enabled.parse(Query.class, """
            SELECT * FROM (SELECT * FROM sales)
            MATCH_RECOGNIZE (PATTERN (A) DEFINE A AS amount > 0) mr
            """);
        var followedByPivot = enabled.parse(Query.class, """
            SELECT * FROM sales
            MATCH_RECOGNIZE (
              MEASURES A.amount AS amount
              PATTERN (A)
              DEFINE A AS A.amount > 0
            ) mr
            PIVOT (sum(amount) FOR amount IN (1)) p
            """);
        var joined = enabled.parse(Query.class, """
            SELECT * FROM lookup l JOIN sales
            MATCH_RECOGNIZE (PATTERN (A) DEFINE A AS amount > 0) mr
            ON l.id = mr.id
            """);

        assertTrue(parenthesized.ok(), parenthesized.errorMessage());
        var parenthesizedPattern = assertInstanceOf(PatternRecognitionTable.class,
            assertInstanceOf(SelectQuery.class, parenthesized.value()).from());
        assertInstanceOf(QueryTable.class, parenthesizedPattern.source());
        assertEquals("mr", parenthesizedPattern.alias().value());

        assertTrue(followedByPivot.ok(), followedByPivot.errorMessage());
        var pivot = assertInstanceOf(PivotTable.class,
            assertInstanceOf(SelectQuery.class, followedByPivot.value()).from());
        assertInstanceOf(PatternRecognitionTable.class, pivot.source());
        assertEquals("p", pivot.alias().value());
        assertEquals("mr", assertInstanceOf(PatternRecognitionTable.class, pivot.source()).alias().value());

        assertTrue(joined.ok(), joined.errorMessage());
        var join = assertInstanceOf(OnJoin.class,
            assertInstanceOf(SelectQuery.class, joined.value()).joins().getFirst());
        var right = assertInstanceOf(PatternRecognitionTable.class, join.right());
        assertEquals("mr", right.alias().value());
    }

    @Test
    void parsesDedicatedClauseAndExpressionNodesDirectly() {
        assertTrue(enabled.parse(PatternMeasure.class, "FIRST(A.amount) AS first_amount").ok());
        assertTrue(enabled.parse(RowsPerMatch.class, "ONE ROW PER MATCH").ok());
        assertEquals(RowsPerMatch.EmptyMatchHandling.SHOW_EMPTY,
            enabled.parse(RowsPerMatch.class, "ALL ROWS PER MATCH SHOW EMPTY MATCHES").value().emptyMatchHandling());
        assertEquals(RowsPerMatch.EmptyMatchHandling.OMIT_EMPTY,
            enabled.parse(RowsPerMatch.class, "ALL ROWS PER MATCH OMIT EMPTY MATCHES").value().emptyMatchHandling());
        assertEquals(RowsPerMatch.EmptyMatchHandling.WITH_UNMATCHED,
            enabled.parse(RowsPerMatch.class, "ALL ROWS PER MATCH WITH UNMATCHED ROWS").value().emptyMatchHandling());
        assertTrue(enabled.parse(AfterMatchSkip.class, "AFTER MATCH SKIP PAST LAST ROW").ok());
        assertTrue(enabled.parse(AfterMatchSkip.class, "AFTER MATCH SKIP TO NEXT ROW").ok());
        assertTrue(enabled.parse(AfterMatchSkip.class, "AFTER MATCH SKIP TO LAST A").ok());
        assertTrue(enabled.parse(PatternSubset.class, "U = (A, B)").ok());
        assertTrue(enabled.parse(PatternDefinition.class, "A AS A.amount > 0").ok());
        assertTrue(enabled.parse(PatternColumnExpr.class, "A.amount").ok());
        assertTrue(enabled.parse(ClassifierExpr.class, "CLASSIFIER(A)").ok());
        assertTrue(enabled.parse(MatchNumberExpr.class, "MATCH_NUMBER()").ok());
        assertTrue(enabled.parse(PatternNavigationExpr.class, "PREV(A.amount, 2)").ok());
        assertTrue(enabled.parse(PatternEvaluationExpr.class, "FINAL LAST(A.amount)").ok());
    }

    @Test
    void rejectsMalformedCommittedPatternSyntaxAndReorderedClauses() {
        assertTrue(enabled.parse(MatchPattern.class, "A |").isError());
        assertTrue(enabled.parse(MatchPattern.Permutation.class, "PERMUTE(A)").isError());
        assertTrue(enabled.parse(MatchPattern.Quantified.class, "A{5,2}").isError());
        assertTrue(enabled.parse(MatchPattern.class, "A{").isError());
        assertTrue(enabled.parse(MatchPattern.Exclusion.class, "{- A }").isError());
        assertTrue(enabled.parse(PatternSubset.class, "U > (A, B)").isError());
        assertTrue(enabled.parse(RowsPerMatch.class, "ALL ROWS PER MATCH SHOW EMPTY ROWS").isError());

        var reordered = enabled.parse(Query.class, """
            SELECT * FROM sales MATCH_RECOGNIZE (
              MEASURES A.amount AS amount
              ORDER BY amount
              PATTERN (A)
              DEFINE A AS A.amount > 0
            )
            """);
        assertTrue(reordered.isError());
    }
}
