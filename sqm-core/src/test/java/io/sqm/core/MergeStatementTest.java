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
import static io.sqm.dsl.Dsl.tbl;
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
    void mergeUpdateAction_requiresAssignments() {
        assertThrows(IllegalArgumentException.class, () -> MergeUpdateAction.of(List.of()));
    }
}
