package io.sqm.core.match;

import io.sqm.core.InsertSource;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.row;
import static io.sqm.dsl.Dsl.rows;
import static io.sqm.dsl.Dsl.select;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InsertSourceMatchTest {

    @Test
    void matchesQueryAndRowShapes() {
        InsertSource query = select(lit(1)).build();
        InsertSource rowList = rows(
            row(lit(1)),
            row(lit(2)));

        assertEquals("QUERY", Match.<String>insertSource(query)
            .query(ignored -> "QUERY")
            .otherwise(ignored -> "OTHER"));

        assertEquals("ROWS", Match.<String>insertSource(rowList)
            .rows(ignored -> "ROWS")
            .otherwise(ignored -> "OTHER"));
    }

    @Test
    void matchesSingleRowAndRowValuesBranches() {
        InsertSource row = row(lit(1));

        assertEquals("ROW", Match.<String>insertSource(row)
            .row(ignored -> "ROW")
            .otherwise(ignored -> "OTHER"));

        assertEquals("ROW-VALUES", Match.<String>insertSource(row)
            .rowValues(ignored -> "ROW-VALUES")
            .otherwise(ignored -> "OTHER"));
    }

    @Test
    void keepsFirstMatchedResult() {
        InsertSource row = row(lit(1));

        var result = Match.<String>insertSource(row)
            .rowValues(ignored -> "FIRST")
            .row(ignored -> "SECOND")
            .otherwise(ignored -> "OTHER");

        assertEquals("FIRST", result);
    }

    @Test
    void fallsBackWhenNoBranchMatches() {
        InsertSource source = select(lit(1)).build();

        var result = Match.<String>insertSource(source)
            .row(ignored -> "ROW")
            .rows(ignored -> "ROWS")
            .otherwise(ignored -> "OTHER");

        assertEquals("OTHER", result);
    }
}
