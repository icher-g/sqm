package io.sqm.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValuesTest {

    @Test
    void single_holds_value() {
        Values.Single s = new Values.Single("x");
        assertEquals("x", s.value());
    }

    @Test
    void listvals_is_deep_immutable_outer_only() {
        List<Object> src = new ArrayList<>(List.of(1, 2, 3));
        Values.ListValues lv = new Values.ListValues(src);
        // mutate original outer list -> should not affect internal list
        src.add(4);
        assertEquals(List.of(1, 2, 3), lv.items());
        assertThrows(UnsupportedOperationException.class, () -> lv.items().add(null));
    }

    @Test
    void tuples_is_deep_immutable() {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(new ArrayList<>(List.of(1, 2)));
        rows.add(new ArrayList<>(List.of(3, 4)));
        Values.Tuples t = new Values.Tuples(rows);

        // external mutations shouldn't affect internal structure
        rows.get(0).set(0, 99);
        rows.add(List.of(5, 6));

        assertEquals(List.of(List.of(1, 2), List.of(3, 4)), t.rows());
        assertThrows(UnsupportedOperationException.class, () -> t.rows().add(null));
        assertThrows(UnsupportedOperationException.class, () -> t.rows().get(0).set(1, null));
    }

    @Test
    void factories_build_expected_shapes() {
        Values.ListValues v1 = Values.list(List.of(1, 2, 3));
        assertInstanceOf(Values.ListValues.class, v1);
        assertEquals(List.of(1,2,3), v1.items());

        Values.Tuples v2 = Values.tuples(Arrays.asList(List.of(1,2), List.of(3,4)));
        assertInstanceOf(Values.Tuples.class, v2);
        assertEquals(List.of(List.of(1,2), List.of(3,4)), v2.rows());

        Values.Range r = Values.range(10, 20);
        assertEquals(10, r.min());
        assertEquals(20, r.max());
    }
}