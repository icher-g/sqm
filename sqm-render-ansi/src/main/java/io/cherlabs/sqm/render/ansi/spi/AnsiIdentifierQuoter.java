package io.cherlabs.sqm.render.ansi.spi;

import io.cherlabs.sqm.render.spi.IdentifierQuoter;

import java.util.Set;
import java.util.regex.Pattern;

public class AnsiIdentifierQuoter implements IdentifierQuoter {
    // Unquoted identifiers must be simple and not reserved:
    private static final Pattern SIMPLE = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    // A pragmatic, compact reserved set (extend if you wish). Dialect owns the knowledge.
    private static final Set<String> RESERVED = Set.of("SELECT", "FROM", "WHERE", "GROUP", "ORDER", "BY", "HAVING", "JOIN", "LEFT", "RIGHT", "FULL", "OUTER", "INNER", "ON", "AS", "AND", "OR", "NOT", "NULL", "TRUE", "FALSE", "IN", "IS", "LIKE", "BETWEEN", "EXISTS", "ALL", "ANY", "UNION", "INTERSECT", "EXCEPT", "DISTINCT", "CASE", "WHEN", "THEN", "ELSE", "END", "LIMIT", "OFFSET", "FETCH", "INSERT", "UPDATE", "DELETE", "CREATE", "ALTER", "DROP", "TABLE", "VIEW", "INDEX");

    @Override
    public String quote(String identifier) {
        // Escape embedded double quotes per SQL standard by doubling them.
        String escaped = identifier.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    @Override
    public String quoteIfNeeded(String identifier) {
        if (needsQuoting(identifier)) return quote(identifier);
        return identifier;
    }

    @Override
    public String qualify(String schema, String name) {
        return quote(schema) + "." + quote(name);
    }

    @Override
    public boolean needsQuoting(String identifier) {
        if (identifier == null || identifier.isEmpty()) return true;
        if (!SIMPLE.matcher(identifier).matches()) return true;
        // ANSI folds unquoted identifiers; keep rule simple: quote reserved words.
        return RESERVED.contains(identifier.toUpperCase());
    }
}
