package io.cherlabs.sqm.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TupleFilterTest {

    @Test
    void tuple_in_via_Filter_tuple_builds_Tuples_values() {
        // (a,b) IN ((1,2),(3,4))
        TupleFilter tcf = Filter
                .tuple(List.of(Column.of("a"), Column.of("b")))
                .in(List.of(List.of(1, 2), List.of(3, 4)));

        assertEquals(TupleFilter.Operator.In, tcf.operator());

        var v = tcf.valuesAs(Values.Tuples.class);
        var rows = v.rows();
        assertEquals(2, rows.size());
        assertEquals(List.of(1, 2), rows.get(0));
        assertEquals(List.of(3, 4), rows.get(1));
    }
}
