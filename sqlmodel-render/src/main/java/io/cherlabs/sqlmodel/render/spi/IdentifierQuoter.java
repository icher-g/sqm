package io.cherlabs.sqlmodel.render.spi;

public interface IdentifierQuoter {
    String quote(String identifier);                            // "\"Order\"" | "[Order]" | "`Order`"
    String quoteIfNeeded(String identifier);
    String qualify(String schemaOrNull, String name);           // "\"s\".\"t\"" | "[s].[t]"
    boolean needsQuoting(String identifier);
}
