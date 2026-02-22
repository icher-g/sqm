package io.sqm.core.internal;

import io.sqm.core.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SelectQueryImplTest {

    @Test
    void testEquals() {
        var q1 = Query.select(Expression.column("c"), Expression.column("d"))
                      .distinct(DistinctSpec.TRUE)
                      .from(TableRef.table("t"))
                      .join(Join.full(TableRef.table("f")))
                      .where(Expression.column("c").eq(1))
                      .groupBy(GroupItem.of(1))
                      .having(Expression.func("n").gt(10))
                      .window(WindowDef.of("w", OverSpec.def(PartitionBy.of(Expression.column("c")), null, null, null)))
                      .orderBy(OrderItem.of(1))
                      .limit(1L)
                      .offset(2L)
                      .build();

        var q2 = Query.select(Expression.column("c"), Expression.column("d"))
                      .distinct(DistinctSpec.TRUE)
                      .from(TableRef.table("t"))
                      .join(Join.full(TableRef.table("f")))
                      .where(Expression.column("c").eq(1))
                      .groupBy(GroupItem.of(1))
                      .having(Expression.func("n").gt(10))
                      .window(WindowDef.of("w", OverSpec.def(PartitionBy.of(Expression.column("c")), null, null, null)))
                      .orderBy(OrderItem.of(1))
                      .limit(1L)
                      .offset(2L)
                      .build();

        assertEquals(q1, q2);
        assertNotEquals(q1, Expression.literal(1));
    }

    @Test
    void testHashCode() {
        var q1 = Query.select(Expression.column("c"), Expression.column("d"))
                      .distinct(DistinctSpec.TRUE)
                      .from(TableRef.table("t"))
                      .join(Join.full(TableRef.table("f")))
                      .where(Expression.column("c").eq(1))
                      .groupBy(GroupItem.of(1))
                      .having(Expression.func("n").gt(10))
                      .window(WindowDef.of("w", OverSpec.def(PartitionBy.of(Expression.column("c")), null, null, null)))
                      .orderBy(OrderItem.of(1))
                      .limit(1L)
                      .offset(2L)
                      .build();

        var q2 = Query.select(Expression.column("c"), Expression.column("d"))
                      .distinct(DistinctSpec.TRUE)
                      .from(TableRef.table("t"))
                      .join(Join.full(TableRef.table("f")))
                      .where(Expression.column("c").eq(1))
                      .groupBy(GroupItem.of(1))
                      .having(Expression.func("n").gt(10))
                      .window(WindowDef.of("w", OverSpec.def(PartitionBy.of(Expression.column("c")), null, null, null)))
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
        builder.select(Expression.column("id"));
        var withSelect = builder.build();
        builder.from(TableRef.table("users")).limit(10);
        var withLimit = builder.build();

        assertNotSame(base, withSelect);
        assertNotSame(withSelect, withLimit);

        assertEquals(0, base.items().size());
        assertEquals(1, withSelect.items().size());
        assertEquals(0, base.joins().size());
        assertEquals(TableRef.table("users"), withLimit.from());
        assertEquals(10L, ((Number) ((LiteralExpr) withLimit.limitOffset().limit()).value()).longValue());
        assertNull(base.limitOffset());
    }

    @Test
    void exposed_lists_are_immutable() {
        var query = Query.select(Expression.column("id"))
            .join(Join.inner(TableRef.table("t2")))
            .window(WindowDef.of("w", OverSpec.def((PartitionBy) null, null, null, null)))
            .build();

        assertThrows(UnsupportedOperationException.class, () -> query.items().add(Expression.column("x").toSelectItem()));
        assertThrows(UnsupportedOperationException.class, () -> query.joins().add(Join.left(TableRef.table("t3"))));
        assertThrows(UnsupportedOperationException.class, () -> query.windows().add(
            WindowDef.of("w2", OverSpec.def((PartitionBy) null, null, null, null))
        ));
    }
}
