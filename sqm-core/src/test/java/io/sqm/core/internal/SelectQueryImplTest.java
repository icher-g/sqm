package io.sqm.core.internal;

import io.sqm.core.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class SelectQueryImplTest {

    @Test
    void testEquals() {
        var q1 = Query.select(col("c"), col("d"))
            .distinct(DistinctSpec.TRUE)
            .from(tbl("t"))
            .join(Join.full(tbl("f")))
            .where(col("c").eq(1))
            .groupBy(GroupItem.of(1))
            .having(func("n").gt(10))
            .window(window("w", OverSpec.def(PartitionBy.of(col("c")), null, null, null)))
            .orderBy(OrderItem.of(1))
            .limit(1L)
            .offset(2L)
            .build();

        var q2 = Query.select(col("c"), col("d"))
            .distinct(DistinctSpec.TRUE)
            .from(tbl("t"))
            .join(Join.full(tbl("f")))
            .where(col("c").eq(1))
            .groupBy(GroupItem.of(1))
            .having(func("n").gt(10))
            .window(window("w", OverSpec.def(PartitionBy.of(col("c")), null, null, null)))
            .orderBy(OrderItem.of(1))
            .limit(1L)
            .offset(2L)
            .build();

        assertEquals(q1, q2);
        assertNotEquals(q1, Expression.literal(1));
    }

    @Test
    void testHashCode() {
        var q1 = Query.select(col("c"), col("d"))
            .distinct(DistinctSpec.TRUE)
            .from(tbl("t"))
            .join(Join.full(tbl("f")))
            .where(col("c").eq(1))
            .groupBy(GroupItem.of(1))
            .having(func("n").gt(10))
            .window(window("w", OverSpec.def(PartitionBy.of(col("c")), null, null, null)))
            .orderBy(OrderItem.of(1))
            .limit(1L)
            .offset(2L)
            .build();

        var q2 = Query.select(col("c"), col("d"))
            .distinct(DistinctSpec.TRUE)
            .from(tbl("t"))
            .join(Join.full(tbl("f")))
            .where(col("c").eq(1))
            .groupBy(GroupItem.of(1))
            .having(func("n").gt(10))
            .window(window("w", OverSpec.def(PartitionBy.of(col("c")), null, null, null)))
            .orderBy(OrderItem.of(1))
            .limit(1L)
            .offset(2L)
            .build();

        assertEquals(q1.hashCode(), q2.hashCode());
    }

    @Test
    void builder_build_produces_immutable_snapshots() {
        var builder = Query.select();
        var base = builder.build();
        builder.select(col("id"));
        var withSelect = builder.build();
        builder.from(tbl("users")).limit(10);
        var withLimit = builder.build();

        assertNotSame(base, withSelect);
        assertNotSame(withSelect, withLimit);

        assertEquals(0, base.items().size());
        assertEquals(1, withSelect.items().size());
        assertEquals(0, base.joins().size());
        assertEquals(tbl("users"), withLimit.from());
        assertEquals(10L, ((Number) ((io.sqm.core.LiteralExpr) withLimit.limitOffset().limit()).value()).longValue());
        assertNull(base.limitOffset());
    }

    @Test
    void exposed_lists_are_immutable() {
        var query = Query.select(col("id"))
            .join(Join.inner(tbl("t2")))
            .window(window("w", OverSpec.def((PartitionBy) null, null, null, null)))
            .build();

        assertThrows(UnsupportedOperationException.class, () -> query.items().add(col("x").toSelectItem()));
        assertThrows(UnsupportedOperationException.class, () -> query.joins().add(Join.left(tbl("t3"))));
        assertThrows(UnsupportedOperationException.class, () -> query.windows().add(
            window("w2", OverSpec.def((PartitionBy) null, null, null, null))
        ));
    }

    @Test
    void modifiers_and_optimizer_hints_are_copied() {
        var query = SelectQuery.of(
            List.of(col("id").toSelectItem()),
            tbl("users"),
            List.of(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of(),
            List.of(SelectModifier.CALC_FOUND_ROWS),
            List.of("MAX_EXECUTION_TIME(1000)")
        );

        assertEquals(SelectModifier.CALC_FOUND_ROWS, query.modifiers().getFirst());
        assertEquals("MAX_EXECUTION_TIME(1000)", query.optimizerHints().getFirst());
        assertThrows(UnsupportedOperationException.class, () -> query.modifiers().add(SelectModifier.CALC_FOUND_ROWS));
        assertThrows(UnsupportedOperationException.class, () -> query.optimizerHints().add("BKA(users)"));
    }

    @Test
    void legacyFactoryUsesEmptyModifierAndHintLists() {
        var query = SelectQuery.of(
            List.of(col("id").toSelectItem()),
            tbl("users"),
            List.of(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of()
        );

        assertEquals(List.of(), query.modifiers());
        assertEquals(List.of(), query.optimizerHints());
    }

    @Test
    void nullModifierAndHintListsDefaultToEmpty() {
        var query = SelectQuery.of(
            List.of(col("id").toSelectItem()),
            tbl("users"),
            List.of(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of(),
            null,
            null
        );

        assertEquals(List.of(), query.modifiers());
        assertEquals(List.of(), query.optimizerHints());
    }
}