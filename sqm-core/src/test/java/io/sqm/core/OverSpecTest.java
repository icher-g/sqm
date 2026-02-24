package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static io.sqm.dsl.Dsl.over;

class OverSpecTest {

    @Test
    void maybeRef() {
        OverSpec spec = over("w");
        assertTrue(spec.<Boolean>matchOverSpec().ref(r -> true).orElse(false));
        assertFalse(OverSpec.def(Identifier.of("w"), OrderBy.of(OrderItem.of(1)), FrameSpec.single(FrameSpec.Unit.ROWS, BoundSpec.currentRow()), OverSpec.Exclude.GROUP).<Boolean>matchOverSpec().ref(r -> true).orElse(false));
    }

    @Test
    void maybeDef() {
        OverSpec spec = OverSpec.def(Identifier.of("w"), OrderBy.of(OrderItem.of(1)), FrameSpec.single(FrameSpec.Unit.ROWS, BoundSpec.currentRow()), OverSpec.Exclude.GROUP);
        assertTrue(spec.<Boolean>matchOverSpec().def(r -> true).orElse(false));
        assertFalse(over("w").<Boolean>matchOverSpec().def(r -> true).orElse(false));
    }
}
