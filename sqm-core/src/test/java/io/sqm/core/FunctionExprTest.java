package io.sqm.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FunctionExprTest {

    @Test
    void of() {
        List<FunctionExpr.Arg> args = List.of(Expression.starArg(), Expression.funcArg(Expression.literal(1)));
        OrderBy withinGroups = OrderBy.of(List.of(OrderItem.of(1)));
        Predicate filter = Expression.column("c").eq(1);
        OverSpec over = OverSpec.ref("w");
        assertInstanceOf(FunctionExpr.class, FunctionExpr.of("rank"));
        assertInstanceOf(FunctionExpr.class, FunctionExpr.of("rank", args, true, withinGroups, filter, over));
    }

    @Test
    void distinct() {
        var f = FunctionExpr.of("count", FunctionExpr.Arg.star()).distinct();
        assertInstanceOf(FunctionExpr.class, f);
        assertTrue(f.distinctArg());
    }

    @Test
    void withinGroup() {
        OrderItem item = OrderItem.of(1);
        OrderBy withinGroups = OrderBy.of(List.of(item));
        var f1 = FunctionExpr.of("count", FunctionExpr.Arg.star()).withinGroup(item);
        assertEquals(withinGroups, f1.withinGroup());
        var f2 = FunctionExpr.of("count", FunctionExpr.Arg.star()).withinGroup(withinGroups);
        assertEquals(withinGroups, f2.withinGroup());
    }

    @Test
    void filter() {
        var predicate = Expression.column("c").eq(1);
        var f = FunctionExpr.of("count", FunctionExpr.Arg.star()).filter(predicate);
        assertEquals(predicate, f.filter());
    }

    @Test
    void over() {
        var f = FunctionExpr.of("count", FunctionExpr.Arg.star());
        assertInstanceOf(OverSpec.Ref.class, f.over("w").over());
        assertInstanceOf(OverSpec.Ref.class, f.over(OverSpec.ref("w")).over());
        // check all variations of: over(PartitionBy partitionBy, OrderBy orderBy, FrameSpec frame, OverSpec.Exclude exclude)
        var over = f.over(PartitionBy.of(Expression.literal(1))).over();
        assertTrue(over.asDef().isPresent());
        assertNotNull(over.asDef().orElseThrow().partitionBy());
        assertEquals(1, over.asDef().orElseThrow().partitionBy().items().size());
        over = f.over(PartitionBy.of(Expression.literal(1)), OrderBy.of(OrderItem.of(1))).over();
        assertTrue(over.asDef().isPresent());
        assertNotNull(over.asDef().orElseThrow().partitionBy());
        assertEquals(1, over.asDef().orElseThrow().partitionBy().items().size());
        assertNotNull(over.asDef().orElseThrow().orderBy());
        assertEquals(1, over.asDef().orElseThrow().orderBy().items().size());
        over = f.over(PartitionBy.of(Expression.literal(1)), FrameSpec.single(FrameSpec.Unit.ROWS, BoundSpec.currentRow())).over();
        assertTrue(over.asDef().isPresent());
        assertNotNull(over.asDef().orElseThrow().partitionBy());
        assertEquals(1, over.asDef().orElseThrow().partitionBy().items().size());
        assertNotNull(over.asDef().orElseThrow().frame());
        assertEquals(FrameSpec.Unit.ROWS, over.asDef().orElseThrow().frame().unit());
        assertInstanceOf(BoundSpec.CurrentRow.class, over.asDef().orElseThrow().frame().asSingle().orElseThrow().bound());
        over = f.over(PartitionBy.of(Expression.literal(1)), OrderBy.of(OrderItem.of(1)), FrameSpec.single(FrameSpec.Unit.ROWS, BoundSpec.currentRow())).over();
        assertTrue(over.asDef().isPresent());
        assertNotNull(over.asDef().orElseThrow().partitionBy());
        assertEquals(1, over.asDef().orElseThrow().partitionBy().items().size());
        assertNotNull(over.asDef().orElseThrow().orderBy());
        assertEquals(1, over.asDef().orElseThrow().orderBy().items().size());
        assertNotNull(over.asDef().orElseThrow().frame());
        assertEquals(FrameSpec.Unit.ROWS, over.asDef().orElseThrow().frame().unit());
        assertInstanceOf(BoundSpec.CurrentRow.class, over.asDef().orElseThrow().frame().asSingle().orElseThrow().bound());
        over = f.over(PartitionBy.of(Expression.literal(1)), OrderBy.of(OrderItem.of(1)), FrameSpec.single(FrameSpec.Unit.ROWS, BoundSpec.currentRow()), OverSpec.Exclude.CURRENT_ROW).over();
        assertTrue(over.asDef().isPresent());
        assertNotNull(over.asDef().orElseThrow().partitionBy());
        assertEquals(1, over.asDef().orElseThrow().partitionBy().items().size());
        assertNotNull(over.asDef().orElseThrow().orderBy());
        assertEquals(1, over.asDef().orElseThrow().orderBy().items().size());
        assertNotNull(over.asDef().orElseThrow().frame());
        assertEquals(FrameSpec.Unit.ROWS, over.asDef().orElseThrow().frame().unit());
        assertInstanceOf(BoundSpec.CurrentRow.class, over.asDef().orElseThrow().frame().asSingle().orElseThrow().bound());
        assertEquals(OverSpec.Exclude.CURRENT_ROW, over.asDef().orElseThrow().exclude());
        // check all variations of: over(String baseWindow, OrderBy orderBy, FrameSpec frame, OverSpec.Exclude exclude)
        over = f.over("w", OrderBy.of(OrderItem.of(1))).over();
        assertTrue(over.asDef().isPresent());
        assertNull(over.asDef().orElseThrow().partitionBy());
        assertEquals("w", over.asDef().orElseThrow().baseWindow());
        assertNotNull(over.asDef().orElseThrow().orderBy());
        assertEquals(1, over.asDef().orElseThrow().orderBy().items().size());
        over = f.over("w", FrameSpec.single(FrameSpec.Unit.ROWS, BoundSpec.currentRow())).over();
        assertTrue(over.asDef().isPresent());
        assertNull(over.asDef().orElseThrow().partitionBy());
        assertEquals("w", over.asDef().orElseThrow().baseWindow());
        assertNotNull(over.asDef().orElseThrow().frame());
        assertEquals(FrameSpec.Unit.ROWS, over.asDef().orElseThrow().frame().unit());
        assertInstanceOf(BoundSpec.CurrentRow.class, over.asDef().orElseThrow().frame().asSingle().orElseThrow().bound());
        over = f.over("w", OrderBy.of(OrderItem.of(1)), FrameSpec.single(FrameSpec.Unit.ROWS, BoundSpec.currentRow())).over();
        assertTrue(over.asDef().isPresent());
        assertNull(over.asDef().orElseThrow().partitionBy());
        assertEquals("w", over.asDef().orElseThrow().baseWindow());
        assertNotNull(over.asDef().orElseThrow().orderBy());
        assertEquals(1, over.asDef().orElseThrow().orderBy().items().size());
        assertNotNull(over.asDef().orElseThrow().frame());
        assertEquals(FrameSpec.Unit.ROWS, over.asDef().orElseThrow().frame().unit());
        assertInstanceOf(BoundSpec.CurrentRow.class, over.asDef().orElseThrow().frame().asSingle().orElseThrow().bound());
        over = f.over("w", OrderBy.of(OrderItem.of(1)), FrameSpec.single(FrameSpec.Unit.ROWS, BoundSpec.currentRow()), OverSpec.Exclude.CURRENT_ROW).over();
        assertTrue(over.asDef().isPresent());
        assertNull(over.asDef().orElseThrow().partitionBy());
        assertEquals("w", over.asDef().orElseThrow().baseWindow());
        assertNotNull(over.asDef().orElseThrow().orderBy());
        assertEquals(1, over.asDef().orElseThrow().orderBy().items().size());
        assertNotNull(over.asDef().orElseThrow().frame());
        assertEquals(FrameSpec.Unit.ROWS, over.asDef().orElseThrow().frame().unit());
        assertInstanceOf(BoundSpec.CurrentRow.class, over.asDef().orElseThrow().frame().asSingle().orElseThrow().bound());
        assertEquals(OverSpec.Exclude.CURRENT_ROW, over.asDef().orElseThrow().exclude());
    }
}