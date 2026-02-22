package io.sqm.core.match;

import io.sqm.core.CompositeQuery;
import io.sqm.core.Query;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class QueryMatchTest {

    @Test
    void select_vs_composite_vs_with() {
        Query query = select(lit(1)).build();

        String out = Match
            .<String>query(query)
            .composite(q -> "COMP")
            .with(q -> "WITH")
            .select(q -> "SELECT")
            .otherwise(q -> "OTHER");

        assertEquals("SELECT", out);

        out = Match
            .<String>query(query.union(query))
            .with(q -> "WITH")
            .composite(q -> "COMP")
            .select(q -> "SELECT")
            .otherwise(q -> "OTHER");

        assertEquals("COMP", out);

        var with = with(cte("n")).body(query);
        out = Match
            .<String>query(with)
            .with(q -> "WITH")
            .composite(q -> "COMP")
            .select(q -> "SELECT")
            .with(q -> "WITH")
            .otherwise(q -> "OTHER");

        assertEquals("WITH", out);

        out = Match
            .<String>query(with)
            .composite(q -> "COMP")
            .select(q -> "SELECT")
            .otherwise(q -> "OTHER");

        assertEquals("OTHER", out);
    }

    @Test
    void orElseThrow_behaves() {
        CompositeQuery comp = select(lit(1)).build().union(select(lit(2)).build());

        assertThrows(UnsupportedOperationException.class, () ->
            QueryMatch
                .<String>match(comp)
                .with(q -> "NOPE")
                .orElseThrow(UnsupportedOperationException::new)
        );
    }
}
