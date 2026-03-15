package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        assertNull(query.topSpec());
        assertNotNull(query.limitOffset());
        assertNotNull(query.lockFor());
    }

    @Test
    void stores_top_spec_in_query() {
        var query = SelectQuery.builder()
            .select(Expression.literal(1))
            .top(TopSpec.of(Expression.literal(10), true, true))
            .build();

        assertNotNull(query.topSpec());
        assertTrue(query.topSpec().percent());
        assertTrue(query.topSpec().withTies());
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
    @Test
    void supports_select_modifiers_and_optimizer_hints() {
        var query = SelectQuery.builder()
            .select(col("id"))
            .from(TableRef.table(Identifier.of("users")))
            .selectModifier(SelectModifier.CALC_FOUND_ROWS)
            .optimizerHint("MAX_EXECUTION_TIME(1000)")
            .build();

        assertEquals(1, query.modifiers().size());
        assertEquals(SelectModifier.CALC_FOUND_ROWS, query.modifiers().getFirst());
        assertEquals(1, query.optimizerHints().size());
        assertEquals("MAX_EXECUTION_TIME(1000)", query.optimizerHints().getFirst());
    }

    @Test
    void select_modifier_and_optimizer_hint_validate_null() {
        var builder = SelectQuery.builder().select(col("id"));

        assertThrows(NullPointerException.class, () -> builder.selectModifier(null));
        assertThrows(NullPointerException.class, () -> builder.optimizerHint(null));
        assertThrows(NullPointerException.class, () -> builder.selectModifiers(null));
        assertThrows(NullPointerException.class, () -> builder.optimizerHints(null));
    }

    @Test
    void builderCanCopyExistingQuery() {
        var original = SelectQuery.builder()
            .select(col("id"))
            .from(TableRef.table(Identifier.of("users")))
            .optimizerHint("MAX_EXECUTION_TIME(1000)")
            .build();

        var copied = SelectQuery.builder(original)
            .clearOptimizerHints()
            .build();

        assertEquals(java.util.List.of("MAX_EXECUTION_TIME(1000)"), original.optimizerHints());
        assertEquals(java.util.List.of(), copied.optimizerHints());
        assertEquals(original.items(), copied.items());
        assertEquals(original.from(), copied.from());
    }

    @Test
    void exposesCurrentBuilderStateForHookBasedParsing() {
        var builder = SelectQuery.builder()
            .distinct(DistinctSpec.TRUE)
            .top(TopSpec.of(Expression.literal(3)))
            .orderBy(OrderItem.of(col("id")))
            .limitOffset(LimitOffset.of(Expression.literal(5), Expression.literal(1)));

        assertNotNull(builder.currentDistinct());
        assertNotNull(builder.currentTopSpec());
        assertNotNull(builder.currentOrderBy());
        assertNotNull(builder.currentLimitOffset());
    }
}
