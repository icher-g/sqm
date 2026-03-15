package io.sqm.render.sqlserver.spi;

import io.sqm.core.QuoteStyle;
import io.sqm.render.ansi.spi.AnsiIdentifierQuoter;
import io.sqm.render.spi.IdentifierQuoter;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * SQL Server identifier quoting rules.
 */
public class SqlServerIdentifierQuoter implements IdentifierQuoter {

    private static final Pattern SIMPLE = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final Set<String> RESERVED;

    static {
        RESERVED = new HashSet<>();
        RESERVED.addAll(AnsiIdentifierQuoter.RESERVED);
        RESERVED.addAll(Set.of(
            "TOP",
            "PERCENT",
            "TIES",
            "MERGE",
            "OUTPUT"
        ));
    }

    private final boolean quotedIdentifierMode;

    /**
     * Creates SQL Server identifier quoter using brackets.
     */
    public SqlServerIdentifierQuoter() {
        this(false);
    }

    /**
     * Creates SQL Server identifier quoter.
     *
     * @param quotedIdentifierMode if {@code true}, double quotes are accepted as identifier quotes.
     */
    public SqlServerIdentifierQuoter(boolean quotedIdentifierMode) {
        this.quotedIdentifierMode = quotedIdentifierMode;
    }

    @Override
    public String quote(String identifier) {
        String escaped = identifier.replace("]", "]]");
        return "[" + escaped + "]";
    }

    /**
     * Quotes identifier using the requested quote style when supported by SQL Server.
     *
     * @param identifier the identifier to quote.
     * @param quoteStyle the requested quote style.
     * @return a quoted identifier.
     */
    @Override
    public String quote(String identifier, QuoteStyle quoteStyle) {
        if (!supports(quoteStyle)) {
            throw new IllegalArgumentException("Unsupported quote style for SQL Server: " + quoteStyle);
        }
        return switch (quoteStyle == null ? QuoteStyle.NONE : quoteStyle) {
            case NONE -> quoteIfNeeded(identifier);
            case BRACKETS -> quote(identifier);
            case DOUBLE_QUOTE -> {
                if (!quotedIdentifierMode) {
                    throw new IllegalArgumentException("DOUBLE_QUOTE is not enabled for SQL Server");
                }
                String escaped = identifier.replace("\"", "\"\"");
                yield "\"" + escaped + "\"";
            }
            default -> throw new IllegalArgumentException("Unsupported quote style for SQL Server: " + quoteStyle);
        };
    }

    @Override
    public String quoteIfNeeded(String identifier) {
        return needsQuoting(identifier) ? quote(identifier) : identifier;
    }

    @Override
    public String qualify(String schemaOrNull, String name) {
        if (schemaOrNull == null || schemaOrNull.isEmpty()) {
            return quote(name);
        }
        return quote(schemaOrNull) + "." + quote(name);
    }

    @Override
    public boolean needsQuoting(String identifier) {
        if (identifier == null || identifier.isEmpty()) return true;
        if (!SIMPLE.matcher(identifier).matches()) return true;
        return RESERVED.contains(identifier.toUpperCase());
    }

    /**
     * Indicates whether the dialect supports the requested quote style.
     *
     * @param quoteStyle the quote style to check.
     * @return true if supported.
     */
    @Override
    public boolean supports(QuoteStyle quoteStyle) {
        if (quoteStyle == null || quoteStyle == QuoteStyle.NONE || quoteStyle == QuoteStyle.BRACKETS) {
            return true;
        }
        return quotedIdentifierMode && quoteStyle == QuoteStyle.DOUBLE_QUOTE;
    }
}
