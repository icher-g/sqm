package io.sqm.render.spi;

/**
 * An interface to provide a string representation of the operators per dialect.
 */
public interface Operators {
    String in();            // "IN"

    String notIn();         // "NOT IN"

    String between();       // expr.g. "BETWEEN" (or emulation if not supported)

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

    String and();           // AND

    String or();            // OR

    String not();           // NOT
}
