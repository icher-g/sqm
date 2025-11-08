package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FrameSpecTest {

    @Test
    void single() {
        var frame = FrameSpec.single(FrameSpec.Unit.ROWS, BoundSpec.currentRow());
        assertEquals(FrameSpec.Unit.ROWS, frame.unit());
        assertInstanceOf(BoundSpec.CurrentRow.class, frame.bound());
    }

    @Test
    void between() {
        var frame = FrameSpec.between(FrameSpec.Unit.ROWS, BoundSpec.unboundedPreceding(), BoundSpec.unboundedFollowing());
        assertEquals(FrameSpec.Unit.ROWS, frame.unit());
        assertInstanceOf(BoundSpec.UnboundedPreceding.class, frame.start());
        assertInstanceOf(BoundSpec.UnboundedFollowing.class, frame.end());
    }

    @Test
    void asSingle() {
        FrameSpec frame = FrameSpec.single(FrameSpec.Unit.ROWS, BoundSpec.currentRow());
        assertTrue(frame.asSingle().isPresent());
        assertFalse(FrameSpec.between(FrameSpec.Unit.ROWS, BoundSpec.unboundedPreceding(), BoundSpec.unboundedFollowing()).asSingle().isPresent());
    }

    @Test
    void asBetween() {
        FrameSpec frame = FrameSpec.between(FrameSpec.Unit.ROWS, BoundSpec.unboundedPreceding(), BoundSpec.unboundedFollowing());
        assertTrue(frame.asBetween().isPresent());
        assertFalse(FrameSpec.single(FrameSpec.Unit.ROWS, BoundSpec.currentRow()).asBetween().isPresent());
    }
}