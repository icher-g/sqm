package io.sqm.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.id;
import static io.sqm.dsl.Dsl.inserted;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.merge;
import static io.sqm.dsl.Dsl.row;
import static io.sqm.dsl.Dsl.set;
import static io.sqm.dsl.Dsl.statementHint;
import static io.sqm.dsl.Dsl.tbl;
import static io.sqm.dsl.Dsl.topPercent;
import static io.sqm.dsl.Dsl.topWithTies;
import static org.junit.jupiter.api.Assertions.*;

class MergeStatementTest {

    @Test
    void builder_createsMergeStatementWithAllFirstSliceBranches() {
        var statement = merge(tbl("users"))
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedUpdate(col("s", "active").eq(lit(true)), List.of(set("name", col("s", "name"))))
            .whenMatchedDelete()
            .whenNotMatchedInsert(col("s", "name").isNotNull(), List.of(id("id"), id("name")), row(col("s", "id"), col("s", "name")))
            .result(inserted("id"))
            .build();

        assertEquals("users", statement.target().name().value());
        assertEquals(3, statement.clauses().size());
        assertNotNull(statement.clauses().getFirst().condition());
        assertNotNull(statement.clauses().get(2).condition());
        assertNotNull(statement.result());
    }

    @Test
    void builder_requiresSourceOnAndClauses() {
        var missingSource = MergeStatement.builder(tbl("users"))
            .on(col("users", "id").eq(col("src", "id")));
        var missingOn = MergeStatement.builder(tbl("users"))
            .source(tbl("src"));
        var missingClauses = MergeStatement.builder(tbl("users"))
            .source(tbl("src"))
            .on(col("users", "id").eq(col("src", "id")));

        assertThrows(IllegalStateException.class, missingSource::build);
        assertThrows(IllegalStateException.class, missingOn::build);
        assertThrows(IllegalStateException.class, missingClauses::build);
    }

    @Test
    void mergeClause_enforcesAllowedMatchTypeAndActionPairs() {
        assertThrows(IllegalArgumentException.class, () -> MergeClause.of(
            MergeClause.MatchType.MATCHED,
            MergeInsertAction.of(List.of(id("id")), row(lit(1L)))
        ));

        assertThrows(IllegalArgumentException.class, () -> MergeClause.of(
            MergeClause.MatchType.NOT_MATCHED,
            MergeDeleteAction.of()
        ));

        assertThrows(IllegalArgumentException.class, () -> MergeClause.of(
            MergeClause.MatchType.NOT_MATCHED_BY_SOURCE,
            MergeInsertAction.of(List.of(id("id")), row(lit(1L)))
        ));
    }

    @Test
    void mergeClause_preservesOptionalCondition() {
        var clause = MergeClause.of(
            MergeClause.MatchType.MATCHED,
            col("s", "active").eq(lit(true)),
            MergeDeleteAction.of()
        );

        assertNotNull(clause.condition());
    }

    @Test
    void mergeClause_withoutExplicitConditionDefaultsToNull() {
        var clause = MergeClause.of(
            MergeClause.MatchType.MATCHED,
            MergeDeleteAction.of()
        );

        assertEquals(MergeClause.MatchType.MATCHED, clause.matchType());
        assertNull(clause.condition());
    }

    @Test
    void mergeInsertAction_normalizesNullColumnsToEmptyList() {
        var action = MergeInsertAction.of(null, row(lit(1L)));

        assertEquals(List.of(), action.columns());
    }

    @Test
    void builder_supportsPredicateAwareVarargAndValueListMergeOverloads() {
        var statement = merge(tbl("users"))
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedUpdate(
                col("s", "active").eq(lit(true)),
                set("name", col("s", "name")),
                set("id", col("s", "id"))
            )
            .whenNotMatchedInsert(
                col("s", "id").gt(lit(0L)),
                row(col("s", "id"))
            )
            .whenNotMatchedInsert(
                col("s", "name").isNotNull(),
                List.of(id("id"), id("name")),
                List.of(col("s", "id"), col("s", "name"))
            )
            .build();

        assertEquals(3, statement.clauses().size());
        assertEquals(2, ((MergeUpdateAction) statement.clauses().getFirst().action()).assignments().size());
        assertEquals(List.of(), ((MergeInsertAction) statement.clauses().get(1).action()).columns());
        assertEquals(2, ((MergeInsertAction) statement.clauses().get(2).action()).values().items().size());
        assertNotNull(statement.clauses().get(1).condition());
        assertNotNull(statement.clauses().get(2).condition());
    }

    @Test
    void builder_supportsNotMatchedBySourceBranches() {
        var statement = merge(tbl("users"))
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenNotMatchedBySourceUpdate(
                col("users", "active").eq(lit(true)),
                set("name", lit("archived"))
            )
            .whenNotMatchedBySourceDelete()
            .build();

        assertEquals(MergeClause.MatchType.NOT_MATCHED_BY_SOURCE, statement.clauses().getFirst().matchType());
        assertInstanceOf(MergeUpdateAction.class, statement.clauses().getFirst().action());
        assertEquals(MergeClause.MatchType.NOT_MATCHED_BY_SOURCE, statement.clauses().get(1).matchType());
        assertInstanceOf(MergeDeleteAction.class, statement.clauses().get(1).action());
    }

    @Test
    void builder_supportsDoNothingBranches() {
        var statement = merge(tbl("users"))
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedDoNothing(col("s", "active").eq(lit(true)))
            .whenNotMatchedDoNothing()
            .whenNotMatchedBySourceDoNothing(col("users", "name").isNotNull())
            .build();

        assertInstanceOf(MergeDoNothingAction.class, statement.clauses().get(0).action());
        assertEquals(MergeClause.MatchType.NOT_MATCHED, statement.clauses().get(1).matchType());
        assertInstanceOf(MergeDoNothingAction.class, statement.clauses().get(1).action());
        assertEquals(MergeClause.MatchType.NOT_MATCHED_BY_SOURCE, statement.clauses().get(2).matchType());
        assertInstanceOf(MergeDoNothingAction.class, statement.clauses().get(2).action());
    }

    @Test
    void builder_supportsMergeTopVariants() {
        var statement = merge(tbl("users"))
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .top(topPercent(lit(10L)))
            .whenMatchedDelete()
            .build();
        var withTies = merge(tbl("users"))
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .top(topWithTies(lit(5L)))
            .whenMatchedDelete()
            .build();

        assertNotNull(statement.topSpec());
        assertTrue(statement.topSpec().percent());
        assertFalse(statement.topSpec().withTies());
        assertNotNull(withTies.topSpec());
        assertTrue(withTies.topSpec().withTies());
    }

    @Test
    void builder_supportsRawClauseTopLongAndResultOverloads() {
        var statement = MergeStatement.builder(tbl("users"))
            .target(tbl("users").as("u"))
            .source(tbl("src").as("s"))
            .on(col("u", "id").eq(col("s", "id")))
            .top(5L)
            .clause(MergeClause.of(MergeClause.MatchType.MATCHED, MergeDeleteAction.of()))
            .result(java.util.List.of(ResultItem.expr(inserted("id"))))
            .build();
        var intoStatement = MergeStatement.builder(tbl("users"))
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedDelete()
            .result(ResultInto.of(tbl("audit"), List.of(id("user_id"))), inserted("id"))
            .build();

        assertEquals("u", statement.target().alias().value());
        assertEquals(5L, ((LiteralExpr) statement.topSpec().count()).value());
        assertNotNull(statement.result());
        assertNotNull(intoStatement.result());
        assertNotNull(intoStatement.result().into());
    }

    @Test
    void mergeUpdateAction_requiresAssignments() {
        assertThrows(IllegalArgumentException.class, () -> MergeUpdateAction.of(List.of()));
    }

    @Test
    void supportsTypedStatementHints() {
        var statement = merge(tbl("users"))
            .hint("MERGE_HINT")
            .hint(statementHint("MAX_EXECUTION_TIME", 1000))
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedDelete()
            .build();

        assertEquals(2, statement.hints().size());
        assertEquals("MERGE_HINT", statement.hints().getFirst().name().value());
        assertEquals("MAX_EXECUTION_TIME", statement.hints().get(1).name().value());
        assertThrows(UnsupportedOperationException.class, () -> statement.hints().add(statementHint("BKA", "users")));
    }

    @Test
    void builderReplacesAndClearsTypedStatementHints() {
        var statement = MergeStatement.builder(tbl("users"))
            .hints(List.of(statementHint("MERGE_HINT"), statementHint("MAX_EXECUTION_TIME", 1000)))
            .clearHints()
            .hint("BKA", "users")
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedDelete()
            .build();

        assertEquals(1, statement.hints().size());
        assertEquals("BKA", statement.hints().getFirst().name().value());
    }

    @Test
    void builderCanCopyExistingStatement() {
        var original = merge(tbl("users"))
            .hint("MERGE_HINT")
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedDelete()
            .build();

        var copied = MergeStatement.builder(original)
            .clearHints()
            .hint("QUERYTRACEON", 4199)
            .build();

        assertEquals(original.target(), copied.target());
        assertEquals(original.source(), copied.source());
        assertEquals(original.on(), copied.on());
        assertEquals(original.clauses(), copied.clauses());
        assertEquals("MERGE_HINT", original.hints().getFirst().name().value());
        assertEquals("QUERYTRACEON", copied.hints().getFirst().name().value());
    }
}
