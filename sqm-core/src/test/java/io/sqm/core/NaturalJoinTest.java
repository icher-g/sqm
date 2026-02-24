package io.sqm.core;

import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.*;

class NaturalJoinTest {

    @Test
    void of() {
        var join = NaturalJoin.of(TableRef.table(Identifier.of("t")));
        assertNotNull(join.right());
        assertTrue(join.right().<Boolean>matchTableRef().table(t -> true).orElse(false));
        assertEquals("t", join.right().matchTableRef().table(t -> t.name().value()).orElse(null));
        join = NaturalJoin.of(tbl("dbo", "t"));
        assertNotNull(join.right());
        assertTrue(join.right().<Boolean>matchTableRef().table(t -> true).orElse(false));
        assertEquals("t", join.right().matchTableRef().table(t -> t.name().value()).orElse(null));
        assertEquals("dbo", join.right().matchTableRef().table(t -> t.schema().value()).orElse(null));
    }
}