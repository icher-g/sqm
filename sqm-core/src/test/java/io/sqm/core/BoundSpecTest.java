package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoundSpecTest {

    @Test
    void unboundedPreceding() {
        assertInstanceOf(BoundSpec.UnboundedPreceding.class, BoundSpec.unboundedPreceding());
    }

    @Test
    void preceding() {
        var preceding = BoundSpec.preceding(Expression.literal(1));
        assertInstanceOf(BoundSpec.Preceding.class, preceding);
        assertEquals(1, preceding.expr().matchExpression().literal(l -> l.value()).orElse(null));
    }

    @Test
    void currentRow() {
        assertInstanceOf(BoundSpec.CurrentRow.class, BoundSpec.currentRow());
    }

    @Test
    void following() {
        var following = BoundSpec.following(Expression.literal(1));
        assertInstanceOf(BoundSpec.Following.class, following);
        assertEquals(1, following.expr().matchExpression().literal(l -> l.value()).orElse(null));
    }

    @Test
    void unboundedFollowing() {
        assertInstanceOf(BoundSpec.UnboundedFollowing.class, BoundSpec.unboundedFollowing());
    }

    @Test
    void maybePreceding() {
        BoundSpec spec = BoundSpec.preceding(Expression.literal(1));
        assertTrue(spec.<Boolean>matchBoundSpec().preceding(p -> true).orElse(false));
        assertFalse(BoundSpec.unboundedFollowing().<Boolean>matchBoundSpec().preceding(p -> true).orElse(false));
    }

    @Test
    void maybeFollowing() {
        BoundSpec spec = BoundSpec.following(Expression.literal(1));
        assertTrue(spec.<Boolean>matchBoundSpec().following(p -> true).orElse(false));
        assertFalse(BoundSpec.unboundedFollowing().<Boolean>matchBoundSpec().following(p -> true).orElse(false));
    }

    @Test
    void maybeCurrentRow() {
        assertTrue(BoundSpec.currentRow().<Boolean>matchBoundSpec().currentRow(p -> true).orElse(false));
        assertFalse(BoundSpec.unboundedFollowing().<Boolean>matchBoundSpec().currentRow(p -> true).orElse(false));
    }

    @Test
    void maybeUnboundedPreceding() {
        assertTrue(BoundSpec.unboundedPreceding().<Boolean>matchBoundSpec().unboundedPreceding(p -> true).orElse(false));
        assertFalse(BoundSpec.unboundedFollowing().<Boolean>matchBoundSpec().unboundedPreceding(p -> true).orElse(false));
    }

    @Test
    void maybeUnboundedFollowing() {
        assertTrue(BoundSpec.unboundedFollowing().<Boolean>matchBoundSpec().unboundedFollowing(p -> true).orElse(false));
        assertFalse(BoundSpec.unboundedPreceding().<Boolean>matchBoundSpec().unboundedFollowing(p -> true).orElse(false));
    }
}