package io.sqm.render.mysql.spi;

import io.sqm.core.QuoteStyle;
import io.sqm.render.ansi.spi.AnsiIdentifierQuoter;
import io.sqm.render.spi.IdentifierQuoter;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * MySQL identifier quoting rules.
 */
public class MySqlIdentifierQuoter implements IdentifierQuoter {

    private static final Pattern SIMPLE = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final Set<String> RESERVED;

    static {
        RESERVED = new HashSet<>();
        RESERVED.addAll(AnsiIdentifierQuoter.RESERVED);
        RESERVED.addAll(Set.of(
            "KEY",
            "INDEX",
            "REGEXP",
            "RLIKE",
            "STRAIGHT_JOIN",
            "LOCK",
            "UNLOCK",
            "JSON",
            "ROW_NUMBER"
        ));
    }

    private final boolean ansiQuotesMode;

    /**
     * Creates MySQL identifier quoter using backticks.
     */
    public MySqlIdentifierQuoter() {
        this(false);
    }

    /**
     * Creates MySQL identifier quoter.
     *
     * @param ansiQuotesMode if {@code true}, double quotes are accepted as identifier quotes.
     */
    public MySqlIdentifierQuoter(boolean ansiQuotesMode) {
        this.ansiQuotesMode = ansiQuotesMode;
    }

    /**
     * Quotes identifier with MySQL backticks.
     *
     * @param identifier the identifier to quote.
     * @return a quoted identifier.
     */
    @Override
    public String quote(String identifier) {
        String escaped = identifier.replace("`", "``");
        return "`" + escaped + "`";
    }

    /**
     * Quotes identifier using requested style when supported by MySQL mode.
     *
     * @param identifier the identifier to quote.
     * @param quoteStyle the requested quote style.
     * @return a quoted identifier.
     */
    @Override
    public String quote(String identifier, QuoteStyle quoteStyle) {
        if (!supports(quoteStyle)) {
            throw new IllegalArgumentException("Unsupported quote style for MySQL: " + quoteStyle);
        }
        return switch (quoteStyle == null ? QuoteStyle.NONE : quoteStyle) {
            case NONE -> quoteIfNeeded(identifier);
            case BACKTICK -> quote(identifier);
            case DOUBLE_QUOTE -> {
                if (!ansiQuotesMode) {
                    throw new IllegalArgumentException("DOUBLE_QUOTE is not enabled for MySQL");
                }
                String escaped = identifier.replace("\"", "\"\"");
                yield "\"" + escaped + "\"";
            }
            default -> throw new IllegalArgumentException("Unsupported quote style for MySQL: " + quoteStyle);
        };
    }

    /**
     * Quotes identifier only when required.
     *
     * @param identifier the identifier to quote.
     * @return quoted or original identifier.
     */
    @Override
    public String quoteIfNeeded(String identifier) {
        return needsQuoting(identifier) ? quote(identifier) : identifier;
    }

    /**
     * Quotes each part of a qualified name.
     *
     * @param schemaOrNull schema part or null.
     * @param name         name part.
     * @return a quoted qualified name.
     */
    @Override
    public String qualify(String schemaOrNull, String name) {
        if (schemaOrNull == null || schemaOrNull.isEmpty()) {
            return quote(name);
        }
        return quote(schemaOrNull) + "." + quote(name);
    }

    /**
     * Indicates if identifier needs quoting.
     *
     * @param identifier the identifier.
     * @return true when quoting is required.
     */
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
        if (quoteStyle == null || quoteStyle == QuoteStyle.NONE || quoteStyle == QuoteStyle.BACKTICK) {
            return true;
        }
        return ansiQuotesMode && quoteStyle == QuoteStyle.DOUBLE_QUOTE;
    }
}
