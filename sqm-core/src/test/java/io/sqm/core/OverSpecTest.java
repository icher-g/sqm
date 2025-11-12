package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverSpecTest {

    @Test
    void maybeRef() {
        OverSpec spec = OverSpec.ref("w");
        assertTrue(spec.<Boolean>matchOverSpec().ref(r -> true).orElse(false));
        assertFalse(OverSpec.def("w", OrderBy.of(OrderItem.of(1)), FrameSpec.single(FrameSpec.Unit.ROWS, BoundSpec.currentRow()), OverSpec.Exclude.GROUP).<Boolean>matchOverSpec().ref(r -> true).orElse(false));
    }

    @Test
    void maybeDef() {
        OverSpec spec = OverSpec.def("w", OrderBy.of(OrderItem.of(1)), FrameSpec.single(FrameSpec.Unit.ROWS, BoundSpec.currentRow()), OverSpec.Exclude.GROUP);
        assertTrue(spec.<Boolean>matchOverSpec().def(r -> true).orElse(false));
        assertFalse(OverSpec.ref("w").<Boolean>matchOverSpec().def(r -> true).orElse(false));
    }
}