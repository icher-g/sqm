package io.sqm.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

public class PredicateTest {
    static Stream<Object[]> inCases() {
        return Stream.of(
            new Object[]{"category", new Object[]{1, 2}, List.of(1, 2)},
            new Object[]{"category", new Object[]{1, 2, 3}, List.of(1, 2, 3)},
            new Object[]{"flags", new Object[]{"A", "B", "C"}, List.of("A", "B", "C")}
        );
    }

    @Test
    void in_builds_List_values_and_sets_operator_In() {
        var p = col("category").in(1, 2, 3);

        assertInstanceOf(InPredicate.class, p);
        assertFalse(p.negated());
        assertEquals("category", p.lhs().asColumn().name());
        assertInstanceOf(RowExpr.class, p.rhs(), "ListExpr for IN");
        var items = p.rhs().asRow().items();
        assertEquals(List.of(1, 2, 3), items.stream().map(e -> e.asLiteral().value()).toList());
    }

    @Test
    void notIn_builds_List_values_and_sets_operator_NotIn() {
        var p = col("status").notIn("A", "B");
        assertInstanceOf(InPredicate.class, p);
        assertTrue(p.negated());
        assertInstanceOf(RowExpr.class, p.rhs(), "ListExpr for NOT IN");
        assertEquals(List.of("A", "B"), p.rhs().asRow().items().stream().map(e -> e.asLiteral().value()).toList());
    }

    @Test
    void like_builds_Single_value_and_sets_operator_Like() {
        var p = col("name").like("%abc%");
        assertInstanceOf(LikePredicate.class, p);
        assertInstanceOf(Expression.class, p.value(), "Expected Expression for value in LIKE");
        assertInstanceOf(Expression.class, p.pattern(), "Expected Expression for pattern in LIKE");
        assertEquals("%abc%", p.pattern().asLiteral().value());
    }

    @Test
    void range_builds_Range_value_and_sets_operator_Ranges() {
        var p = col("price").between(10, 20);
        assertInstanceOf(BetweenPredicate.class, p);
        assertInstanceOf(Expression.class, p.lower(), "Expected Expression for lower() in BETWEEN");
        assertInstanceOf(Expression.class, p.upper(), "Expected Expression for upper() in BETWEEN");
        assertEquals(10, p.lower().asLiteral().value());
        assertEquals(20, p.upper().asLiteral().value());
    }

    @Test
    void tuple_in_via_Filter_tuple_builds_Tuples_values() {
        // (a,b) IN ((1,2),(3,4))
        var p = row(col("a"), col("b"))
            .in(rows(
                row(1, 2),
                row(3, 4))
            );

        assertInstanceOf(InPredicate.class, p);
        var rows = p.rhs().asRows().rows();
        assertEquals(2, rows.size());
        assertEquals(List.of(1, 2), rows.get(0).items().stream().map(e -> e.asLiteral().value()).toList());
        assertEquals(List.of(3, 4), rows.get(1).items().stream().map(e -> e.asLiteral().value()).toList());
    }

    @Nested
    @DisplayName("IN parameterized")
    class InMatrix {

        @ParameterizedTest(name = "[{index}] {0} -> {1}")
        @MethodSource("io.sqm.core.PredicateTest#inCases")
        void in_various(String columnName, Object[] args, List<Object> expected) {
            var p = col(columnName).in(args);
            assertInstanceOf(InPredicate.class, p);
            assertFalse(p.negated());
            assertInstanceOf(RowExpr.class, p.rhs(), "ListExpr for IN");
            var items = p.rhs().asRow().items();
            assertEquals(expected, items.stream().map(e -> e.asLiteral().value()).toList());
        }
    }
}
