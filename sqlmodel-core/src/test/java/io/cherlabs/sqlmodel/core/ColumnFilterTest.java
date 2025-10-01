package io.cherlabs.sqlmodel.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class ColumnFilterTest {
    static Stream<Object[]> inCases() {
        return Stream.of(
                new Object[]{"category", new Object[]{1, 2}, List.of(1, 2)},
                new Object[]{"category", new Object[]{1, 2, 3}, List.of(1, 2, 3)},
                new Object[]{"flags", new Object[]{"A", "B", "C"}, List.of("A", "B", "C")}
        );
    }

    @Test
    void in_builds_List_values_and_sets_operator_In() {
        ColumnFilter cf = Filter.column(Column.of("category")).in(1, 2, 3);

        assertEquals(ColumnFilter.Operator.In, cf.op());
        assertEquals("category", cf.columnAs(NamedColumn.class).name());

        Values v = cf.values();
        assertInstanceOf(Values.ListValues.class, v, "Expected Values.ListVals for IN");
        var items = ((Values.ListValues) v).items();
        assertEquals(List.of(1, 2, 3), items);
    }

    @Test
    void notIn_builds_List_values_and_sets_operator_NotIn() {
        ColumnFilter cf = Filter.column(Column.of("status")).notIn(List.of("A", "B"));
        assertEquals(ColumnFilter.Operator.NotIn, cf.op());

        var v = cf.values();
        assertInstanceOf(Values.ListValues.class, v);
        assertEquals(List.of("A", "B"), ((Values.ListValues) v).items());
    }

    @Test
    void like_builds_Single_value_and_sets_operator_Like() {
        ColumnFilter cf = Filter.column(Column.of("name")).like("%abc%");
        assertEquals(ColumnFilter.Operator.Like, cf.op());

        var v = cf.values();
        assertInstanceOf(Values.Single.class, v, "Expected Values.Single for LIKE");
        assertEquals("%abc%", ((Values.Single) v).value());
    }

    @Test
    void range_builds_Range_value_and_sets_operator_Ranges() {
        ColumnFilter cf = Filter.column(Column.of("price")).range(10, 20);
        assertEquals(ColumnFilter.Operator.Range, cf.op());

        var v = cf.values();
        assertInstanceOf(Values.Range.class, v, "Expected Values.Range for range()");
        assertEquals(10, ((Values.Range) v).min());
        assertEquals(20, ((Values.Range) v).max());
    }

    @Nested
    @DisplayName("IN parameterized")
    class InMatrix {

        @ParameterizedTest(name = "[{index}] {0} -> {1}")
        @MethodSource("io.cherlabs.sqlmodel.core.ColumnFilterTest#inCases")
        void in_various(String columnName, Object[] args, List<Object> expected) {
            ColumnFilter cf = Filter.column(Column.of(columnName)).in(args);
            assertEquals(ColumnFilter.Operator.In, cf.op());
            assertInstanceOf(Values.ListValues.class, cf.values());
            assertEquals(expected, ((Values.ListValues) cf.values()).items());
        }
    }
}
