package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.window;

class SelectQueryBuilderTest {

    @Test
    void builds_select_query_with_incremental_mutations() {
        var query = SelectQuery.builder()
            .select(java.util.List.of(col("id").toSelectItem()))
            .from(TableRef.table(Identifier.of("users")))
            .join(Join.left(TableRef.table(Identifier.of("roles"))))
            .where(col("id").gt(0))
            .groupBy(java.util.List.of(GroupItem.of(col("id"))))
            .having(Expression.literal(1).gt(0))
            .window(window("w", OverSpec.def((PartitionBy) null, null, null, null)))
            .orderBy(java.util.List.of(OrderItem.of(col("id"))))
            .limitOffset(LimitOffset.limit(10))
            .lockFor(LockingClause.of(LockMode.UPDATE, java.util.List.of(), false, false))
            .build();

        assertEquals(1, query.items().size());
        assertNotNull(query.from());
        assertEquals(1, query.joins().size());
        assertNotNull(query.where());
        assertNotNull(query.groupBy());
        assertNotNull(query.having());
        assertEquals(1, query.windows().size());
        assertNotNull(query.orderBy());
        assertNotNull(query.limitOffset());
        assertNotNull(query.lockFor());
    }

    @Test
    void build_produces_immutable_query_and_builder_validates_null_append_inputs() {
        var builder = SelectQuery.builder()
            .select(java.util.List.of(Expression.literal(1).toSelectItem()));
        var built = builder.build();

        assertThrows(UnsupportedOperationException.class, () -> built.items().add(Expression.literal(2).toSelectItem()));
        assertThrows(NullPointerException.class, () -> builder.select((java.util.List<SelectItem>) null));
        assertThrows(NullPointerException.class, () -> builder.join((Join) null));
        assertThrows(NullPointerException.class, () -> builder.window((WindowDef) null));
    }
}


