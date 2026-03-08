package io.sqm.core.match;

import io.sqm.core.Statement;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.delete;
import static io.sqm.dsl.Dsl.insert;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.row;
import static io.sqm.dsl.Dsl.select;
import static io.sqm.dsl.Dsl.set;
import static io.sqm.dsl.Dsl.tbl;
import static io.sqm.dsl.Dsl.update;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StatementMatchTest {

    @Test
    void matchesQueryStatements() {
        Statement statement = select(lit(1)).build();

        var out = Match
            .<String>statement(statement)
            .query(query -> "QUERY")
            .otherwise(ignored -> "OTHER");

        assertEquals("QUERY", out);
    }

    @Test
    void matchesInsertStatements() {
        Statement statement = insert(tbl("users"))
            .values(row(lit(1)))
            .build();

        var out = Match
            .<String>statement(statement)
            .insert(insert -> "INSERT")
            .otherwise(ignored -> "OTHER");

        assertEquals("INSERT", out);
    }

    @Test
    void matchesUpdateStatements() {
        Statement statement = update(tbl("users"))
            .set(set("name", lit("alice")))
            .build();

        var out = Match
            .<String>statement(statement)
            .update(update -> "UPDATE")
            .otherwise(ignored -> "OTHER");

        assertEquals("UPDATE", out);
    }

    @Test
    void matchesDeleteStatements() {
        Statement statement = delete(tbl("users"))
            .build();

        var out = Match
            .<String>statement(statement)
            .delete(delete -> "DELETE")
            .otherwise(ignored -> "OTHER");

        assertEquals("DELETE", out);
    }

    @Test
    void fallsBackWhenNoBranchMatches() {
        Statement statement = select(lit(1)).build();

        var out = StatementMatch
            .<String>match(statement)
            .otherwise(ignored -> "OTHER");

        assertEquals("OTHER", out);
    }

    @Test
    void orElseThrowUsesFallbackForUnmatchedStatement() {
        Statement statement = select(lit(1)).build();

        assertThrows(UnsupportedOperationException.class, () ->
            StatementMatch
                .<String>match(statement)
                .orElseThrow(UnsupportedOperationException::new)
        );
    }
}
