package io.sqm.render.ansi.spi;

import io.sqm.core.Direction;
import io.sqm.core.Nulls;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnsiNullSortingTest {

    private final AnsiNullSorting sorting = new AnsiNullSorting();

    @Test
    void supportsExplicitNullOrdering() {
        assertTrue(sorting.supportsExplicit());
    }

    @Test
    void keywordsForNullPositions() {
        assertEquals("NULLS FIRST", sorting.keyword(Nulls.FIRST));
        assertEquals("NULLS LAST", sorting.keyword(Nulls.LAST));
        assertEquals("", sorting.keyword(Nulls.DEFAULT));
    }

    @Test
    void defaultsMatchAnsiSortingBehavior() {
        assertEquals(Nulls.LAST, sorting.defaultFor(Direction.ASC));
        assertEquals(Nulls.FIRST, sorting.defaultFor(Direction.DESC));
    }
}
