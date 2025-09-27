package io.cherlabs.sqlmodel.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QueryColumnTest {

    @Test
    void as_sets_alias() {
        Query sub = new Query();
        QueryColumn qc = new QueryColumn(sub, null);
        QueryColumn a = qc.as("q");
        assertSame(sub, a.query());
        assertEquals("q", a.alias());
        assertNotSame(qc, a);
    }
}