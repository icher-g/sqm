package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NaturalJoinTest {

    @Test
    void of() {
        var join = NaturalJoin.of(TableRef.table("t"));
        assertNotNull(join.right());
        assertTrue(join.right().<Boolean>matchTableRef().table(t -> true).orElse(false));
        assertEquals("t", join.right().matchTableRef().table(t -> t.name()).orElse(null));
        join = NaturalJoin.of("dbo", "t");
        assertNotNull(join.right());
        assertTrue(join.right().<Boolean>matchTableRef().table(t -> true).orElse(false));
        assertEquals("t", join.right().matchTableRef().table(t -> t.name()).orElse(null));
        assertEquals("dbo", join.right().matchTableRef().table(t -> t.schema()).orElse(null));
    }
}