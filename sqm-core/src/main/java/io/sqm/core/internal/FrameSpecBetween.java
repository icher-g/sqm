package io.sqm.core.internal;

import io.sqm.core.BoundSpec;
import io.sqm.core.FrameSpec;

/**
 * ROWS BETWEEN <start> AND <end>
 *
 * @param unit  a frame unit.
 * @param start a frame start bound.
 * @param end   a frame end bound.
 */
public record FrameSpecBetween(FrameSpec.Unit unit, BoundSpec start, BoundSpec end) implements FrameSpec.Between {
}
