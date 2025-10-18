package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CompositeFilterTest {

    @Test
    void composite_and_where_chaining_collects_filters() {
        var f1 = Filter.column(Column.of("status")).in("A", "B");
        var f2 = Filter.column(Column.of("price")).range(10, 20);
        var f3 = Filter.column(Column.of("name")).like("%pro%");

        CompositeFilter and = Filter.and(f1, f2, f3);

        assertEquals(CompositeFilter.Operator.And, and.op());
        assertNotNull(and.filters());
        assertEquals(3, and.filters().size());
        assertSame(f1, and.filters().get(0));
        assertSame(f2, and.filters().get(1));
        assertSame(f3, and.filters().get(2));
    }
}
