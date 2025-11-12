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
    void maybeSingle() {
        FrameSpec frame = FrameSpec.single(FrameSpec.Unit.ROWS, BoundSpec.currentRow());
        assertTrue(frame.<Boolean>matchFrameSpec().single(s -> true).orElse(false));
        assertFalse(FrameSpec.between(FrameSpec.Unit.ROWS, BoundSpec.unboundedPreceding(), BoundSpec.unboundedFollowing()).<Boolean>matchFrameSpec().single(s -> true).orElse(false));
    }

    @Test
    void maybeBetween() {
        FrameSpec frame = FrameSpec.between(FrameSpec.Unit.ROWS, BoundSpec.unboundedPreceding(), BoundSpec.unboundedFollowing());
        assertTrue(frame.<Boolean>matchFrameSpec().between(s -> true).orElse(false));
        assertFalse(FrameSpec.single(FrameSpec.Unit.ROWS, BoundSpec.currentRow()).<Boolean>matchFrameSpec().between(s -> true).orElse(false));
    }
}