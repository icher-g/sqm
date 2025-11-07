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
        assertEquals(1, preceding.expr().asLiteral().orElseThrow().value());
    }

    @Test
    void currentRow() {
        assertInstanceOf(BoundSpec.CurrentRow.class, BoundSpec.currentRow());
    }

    @Test
    void following() {
        var following = BoundSpec.following(Expression.literal(1));
        assertInstanceOf(BoundSpec.Following.class, following);
        assertEquals(1, following.expr().asLiteral().orElseThrow().value());
    }

    @Test
    void unboundedFollowing() {
        assertInstanceOf(BoundSpec.UnboundedFollowing.class, BoundSpec.unboundedFollowing());
    }

    @Test
    void asPreceding() {
        BoundSpec spec = BoundSpec.preceding(Expression.literal(1));
        assertTrue(spec.asPreceding().isPresent());
        assertFalse(BoundSpec.unboundedFollowing().asPreceding().isPresent());
    }

    @Test
    void asFollowing() {
        BoundSpec spec = BoundSpec.following(Expression.literal(1));
        assertTrue(spec.asFollowing().isPresent());
        assertFalse(BoundSpec.unboundedFollowing().asFollowing().isPresent());
    }

    @Test
    void asCurrentRow() {
        assertTrue(BoundSpec.currentRow().asCurrentRow().isPresent());
        assertFalse(BoundSpec.unboundedFollowing().asCurrentRow().isPresent());
    }

    @Test
    void asUnboundedPreceding() {
        assertTrue(BoundSpec.unboundedPreceding().asUnboundedPreceding().isPresent());
        assertFalse(BoundSpec.unboundedFollowing().asUnboundedPreceding().isPresent());
    }

    @Test
    void asUnboundedFollowing() {
        assertTrue(BoundSpec.unboundedFollowing().asUnboundedFollowing().isPresent());
        assertFalse(BoundSpec.unboundedPreceding().asUnboundedFollowing().isPresent());
    }
}