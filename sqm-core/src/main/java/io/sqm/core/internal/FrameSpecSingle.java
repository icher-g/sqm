package io.sqm.core.internal;

import io.sqm.core.BoundSpec;
import io.sqm.core.FrameSpec;

/**
 * ROWS <bound>  (shorthand for BETWEEN <bound> AND CURRENT ROW in many engines is NOT standard; keep explicit)
 *
 * @param unit  a frame unit.
 * @param bound a frame bound.
 */
public record FrameSpecSingle(Unit unit, BoundSpec bound) implements FrameSpec.Single {
}
