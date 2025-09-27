package io.cherlabs.sqlmodel.render.spi;

public interface Operators {
    String in();            // "IN"
    String notIn();         // "NOT IN"
    String range();         // e.g. "BETWEEN" (or emulation if not supported)

    String eq();            // "="
    String ne();            // "<>" or "!="
    String lt();            // "<"
    String lte();           // "<="
    String gt();            // ">"
    String gte();           // ">="

    String like();          // "LIKE"
    String notLike();       // "NOT LIKE"

    String isNull();        // "IS NULL"
    String isNotNull();     // "IS NOT NULL"

    String and();
    String or();
    String not();
}
