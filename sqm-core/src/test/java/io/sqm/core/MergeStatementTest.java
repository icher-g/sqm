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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MergeStatementTest {

    @Test
    void builder_createsMergeStatementWithAllFirstSliceBranches() {
        var statement = merge(tbl("users"))
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedUpdate(List.of(set("name", col("s", "name"))))
            .whenMatchedDelete()
            .whenNotMatchedInsert(List.of(id("id"), id("name")), row(col("s", "id"), col("s", "name")))
            .result(inserted("id"))
            .build();

        assertEquals("users", statement.target().name().value());
        assertEquals(3, statement.clauses().size());
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
    void mergeInsertAction_normalizesNullColumnsToEmptyList() {
        var action = MergeInsertAction.of(null, row(lit(1L)));

        assertEquals(List.of(), action.columns());
    }

    @Test
    void mergeUpdateAction_requiresAssignments() {
        assertThrows(IllegalArgumentException.class, () -> MergeUpdateAction.of(List.of()));
    }
}
