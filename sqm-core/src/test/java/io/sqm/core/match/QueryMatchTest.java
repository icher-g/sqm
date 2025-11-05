package io.sqm.core.match;

import io.sqm.core.CompositeQuery;
import io.sqm.core.SelectQuery;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class QueryMatchTest {

    @Test
    void select_vs_composite_vs_with() {
        SelectQuery select = select(sel(lit(1)));

        String out = QueryMatch
                         .<String>match(select)
                         .composite(q -> "COMP")
                         .with(q -> "WITH")
                         .select(q -> "SELECT")
                         .otherwise(q -> "OTHER");

        assertEquals("SELECT", out);
    }

    @Test
    void orElseThrow_behaves() {
        CompositeQuery comp = select(sel(lit(1))).union(select(sel(lit(2))));

        assertThrows(UnsupportedOperationException.class, () ->
                                                              QueryMatch
                                                                  .<String>match(comp)
                                                                  .with(q -> "NOPE")
                                                                  .orElseThrow(UnsupportedOperationException::new)
        );
    }
}
