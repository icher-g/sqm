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
        InsertSource rows = rows(
            row(lit(1)),
            row(lit(2)));

        assertEquals("QUERY", Match.<String>insertSource(query)
            .query(ignored -> "QUERY")
            .otherwise(ignored -> "OTHER"));

        assertEquals("ROWS", Match.<String>insertSource(rows)
            .rows(ignored -> "ROWS")
            .otherwise(ignored -> "OTHER"));
    }
}
