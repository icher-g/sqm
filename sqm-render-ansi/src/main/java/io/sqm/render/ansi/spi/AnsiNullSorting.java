package io.sqm.render.ansi.spi;

import io.sqm.core.Direction;
import io.sqm.render.spi.NullSorting;
import io.sqm.core.Nulls;

public class AnsiNullSorting implements NullSorting {
    @Override
    public boolean supportsExplicit() {
        return true;
    }

    @Override
    public String keyword(Nulls n) {
        return switch (n) {
            case First -> "NULLS FIRST";
            case Last -> "NULLS LAST";
            case Default -> ""; // caller should omit when DEFAULT
        };
    }

    @Override
    public Nulls defaultFor(Direction dir) {
        // Typical SQL engine behavior adopted by many systems:
        return (dir == Direction.Asc) ? Nulls.Last : Nulls.First;
    }
}
