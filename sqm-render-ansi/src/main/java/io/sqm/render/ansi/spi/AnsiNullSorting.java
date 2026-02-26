package io.sqm.render.ansi.spi;

import io.sqm.core.Direction;
import io.sqm.core.Nulls;
import io.sqm.render.spi.NullSorting;

/**
 * ANSI null sorting behavior.
 */
public class AnsiNullSorting implements NullSorting {
    /**
     * Creates ANSI null-sorting behavior definition.
     */
    public AnsiNullSorting() {
    }

    @Override
    public boolean supportsExplicit() {
        return true;
    }

    @Override
    public String keyword(Nulls n) {
        return switch (n) {
            case FIRST -> "NULLS FIRST";
            case LAST -> "NULLS LAST";
            case DEFAULT -> ""; // caller should omit when DEFAULT
        };
    }

    @Override
    public Nulls defaultFor(Direction dir) {
        // Typical SQL engine behavior adopted by many systems:
        return (dir == Direction.ASC) ? Nulls.LAST : Nulls.FIRST;
    }
}
