package io.sqm.core.internal;

import io.sqm.core.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SelectQueryImplTest {

    @Test
    void testEquals() {
        var q1 = Query.select(Expression.column("c"), Expression.column("d"))
                      .distinct(true)
                      .from(TableRef.table("t"))
                      .join(Join.full(TableRef.table("f")))
                      .where(Expression.column("c").eq(1))
                      .groupBy(GroupItem.of(1))
                      .having(Expression.func("n").gt(10))
                      .window(WindowDef.of("w", OverSpec.def(PartitionBy.of(Expression.column("c")), null, null, null)))
                      .orderBy(OrderItem.of(1))
                      .limit(1L)
                      .offset(2L);

        var q2 = Query.select(Expression.column("c"), Expression.column("d"))
                      .distinct(true)
                      .from(TableRef.table("t"))
                      .join(Join.full(TableRef.table("f")))
                      .where(Expression.column("c").eq(1))
                      .groupBy(GroupItem.of(1))
                      .having(Expression.func("n").gt(10))
                      .window(WindowDef.of("w", OverSpec.def(PartitionBy.of(Expression.column("c")), null, null, null)))
                      .orderBy(OrderItem.of(1))
                      .limit(1L)
                      .offset(2L);

        assertEquals(q1, q2);
        assertNotEquals(q1, Expression.literal(1));
    }

    @Test
    void testHashCode() {
        var q1 = Query.select(Expression.column("c"), Expression.column("d"))
                      .distinct(true)
                      .from(TableRef.table("t"))
                      .join(Join.full(TableRef.table("f")))
                      .where(Expression.column("c").eq(1))
                      .groupBy(GroupItem.of(1))
                      .having(Expression.func("n").gt(10))
                      .window(WindowDef.of("w", OverSpec.def(PartitionBy.of(Expression.column("c")), null, null, null)))
                      .orderBy(OrderItem.of(1))
                      .limit(1L)
                      .offset(2L);

        var q2 = Query.select(Expression.column("c"), Expression.column("d"))
                      .distinct(true)
                      .from(TableRef.table("t"))
                      .join(Join.full(TableRef.table("f")))
                      .where(Expression.column("c").eq(1))
                      .groupBy(GroupItem.of(1))
                      .having(Expression.func("n").gt(10))
                      .window(WindowDef.of("w", OverSpec.def(PartitionBy.of(Expression.column("c")), null, null, null)))
                      .orderBy(OrderItem.of(1))
                      .limit(1L)
                      .offset(2L);

        assertEquals(q1.hashCode(), q2.hashCode());
    }
}