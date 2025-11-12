package io.sqm.core.match;

import io.sqm.core.BoundSpec;
import io.sqm.core.FrameSpec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FrameSpecMatchTest {

    @Test
    void single() {
        var frame = FrameSpec.single(FrameSpec.Unit.RANGE, BoundSpec.currentRow());
        String out  = Match
            .<String>frameSpec(frame)
            .single(s -> "S")
            .between(b -> "B")
            .single(s -> "S")
            .orElse("ELSE");

        assertEquals("S", out);
    }

    @Test
    void between() {
        var frame = FrameSpec.between(FrameSpec.Unit.GROUPS, BoundSpec.currentRow(), BoundSpec.unboundedFollowing());
        String out  = Match
            .<String>frameSpec(frame)
            .between(b -> "B")
            .single(s -> "S")
            .between(b -> "B")
            .orElse("ELSE");

        assertEquals("B", out);
    }

    @Test
    void otherwise() {
        var frame = FrameSpec.single(FrameSpec.Unit.RANGE, BoundSpec.currentRow());
        String out  = Match
            .<String>frameSpec(frame)
            .between(b -> "B")
            .otherwise(f -> "ELSE");

        assertEquals("ELSE", out);
    }
}