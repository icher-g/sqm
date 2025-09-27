package io.cherlabs.sqlmodel.render.spi;

public interface SqlDialect {
    String name();
    IdentifierQuoter quoter();        // how identifiers are quoted/qualified
    ValueFormatter formatter();       // formats a value into a string
    Placeholders placeholders();      // parameter placeholder style
    Operators operators();            // tokens for arithmetic/comparison/string ops
    Booleans booleans();              // boolean literals & predicate rules
    NullSorting nullSorting();        // null ordering policy and emulation
    PaginationStyle paginationStyle();// LIMIT/OFFSET, TOP, FETCH, etc.
    RenderersRepository renderers();
}
