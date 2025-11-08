package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OverSpecTest {

    @Test
    void asRef() {
        OverSpec spec = OverSpec.ref("w");
        assertTrue(spec.asRef().isPresent());
        assertFalse(OverSpec.def("w", OrderBy.of(OrderItem.of(1)), FrameSpec.single(FrameSpec.Unit.ROWS, BoundSpec.currentRow()), OverSpec.Exclude.GROUP).asRef().isPresent());
    }

    @Test
    void asDef() {
        OverSpec spec = OverSpec.def("w", OrderBy.of(OrderItem.of(1)), FrameSpec.single(FrameSpec.Unit.ROWS, BoundSpec.currentRow()), OverSpec.Exclude.GROUP);
        assertTrue(spec.asDef().isPresent());
        assertFalse(OverSpec.ref("w").asDef().isPresent());
    }
}