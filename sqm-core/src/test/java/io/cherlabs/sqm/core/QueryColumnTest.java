package io.cherlabs.sqm.core;

import org.junit.jupiter.api.Test;

import static io.cherlabs.sqm.dsl.Dsl.query;
import static org.junit.jupiter.api.Assertions.*;

class QueryColumnTest {

    @Test
    void as_sets_alias() {
        Query<?> sub = query();
        QueryColumn qc = new QueryColumn(sub, null);
        QueryColumn a = qc.as("q");
        assertSame(sub, a.query());
        assertEquals("q", a.alias());
        assertNotSame(qc, a);
    }
}