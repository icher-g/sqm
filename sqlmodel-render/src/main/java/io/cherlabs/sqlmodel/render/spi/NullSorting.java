package io.cherlabs.sqlmodel.render.spi;

import io.cherlabs.sqlmodel.core.Direction;
import io.cherlabs.sqlmodel.core.Nulls;

public interface NullSorting {
    boolean supportsExplicit();                 // PG/Oracle true; MySQL/SQLServer false
    String keyword(Nulls n);                    // "NULLS FIRST"/"NULLS LAST" (if supported)
    Nulls defaultFor(Direction dir);            // ASC -> LAST, DESC -> FIRST (typical)
}
