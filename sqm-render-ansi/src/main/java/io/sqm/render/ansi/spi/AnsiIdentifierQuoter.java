package io.sqm.render.ansi.spi;

import io.sqm.core.QuoteStyle;
import io.sqm.render.spi.IdentifierQuoter;

import java.util.Set;
import java.util.regex.Pattern;

public class AnsiIdentifierQuoter implements IdentifierQuoter {
    // A pragmatic, compact reserved set (extend if you wish). Dialect owns the knowledge.
    public static final Set<String> RESERVED = Set.of(
        "SELECT", "FROM", "WHERE", "GROUP", "ORDER", "BY", "HAVING", "JOIN", "LEFT", "RIGHT", "FULL", "OUTER", "INNER", "ON", "AS", "AND", "OR", "NOT", "NULL",
        "TRUE", "FALSE", "IN", "IS", "LIKE", "BETWEEN", "EXISTS", "ALL", "ANY", "UNION", "INTERSECT", "EXCEPT", "DISTINCT", "CASE", "WHEN", "THEN", "ELSE", "END",
        "LIMIT", "OFFSET", "FETCH", "INSERT", "UPDATE", "DELETE", "CREATE", "ALTER", "DROP", "TABLE", "VIEW", "INDEX", "WITH", "RECURSIVE", "WINDOW", "OVER",
        "PARTITION", "RANGE", "ROWS", "CROSS", "NATURAL", "USING", "VALUES", "CAST");

    // Unquoted identifiers must be simple and not reserved:
    private static final Pattern SIMPLE = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    @Override
    public String quote(String identifier) {
        // Escape embedded double quotes per SQL standard by doubling them.
        String escaped = identifier.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    /**
     * Quotes identifier using the requested quote style when supported by ANSI.
     *
     * @param identifier the identifier to quote.
     * @param quoteStyle the requested quote style.
     * @return a quoted identifier.
     */
    @Override
    public String quote(String identifier, QuoteStyle quoteStyle) {
        if (!supports(quoteStyle)) {
            throw new IllegalArgumentException("Unsupported quote style for ANSI: " + quoteStyle);
        }
        return switch (quoteStyle == null ? QuoteStyle.NONE : quoteStyle) {
            case NONE -> quoteIfNeeded(identifier);
            case DOUBLE_QUOTE -> quote(identifier);
            default -> throw new IllegalArgumentException("Unsupported quote style for ANSI: " + quoteStyle);
        };
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

    /**
     * Indicates whether ANSI supports the requested quote style.
     *
     * @param quoteStyle the quote style to check.
     * @return True for SQL standard double quotes and unquoted identifiers.
     */
    @Override
    public boolean supports(QuoteStyle quoteStyle) {
        return quoteStyle == null || quoteStyle == QuoteStyle.NONE || quoteStyle == QuoteStyle.DOUBLE_QUOTE;
    }
}
