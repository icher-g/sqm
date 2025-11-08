package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NaturalJoinTest {

    @Test
    void of() {
        var join = NaturalJoin.of(TableRef.table("t"));
        assertNotNull(join.right());
        assertTrue(join.right().asTable().isPresent());
        assertEquals("t", join.right().asTable().orElseThrow().name());
        join = NaturalJoin.of("dbo", "t");
        assertNotNull(join.right());
        assertTrue(join.right().asTable().isPresent());
        assertEquals("t", join.right().asTable().orElseThrow().name());
        assertEquals("dbo", join.right().asTable().orElseThrow().schema());
    }
}