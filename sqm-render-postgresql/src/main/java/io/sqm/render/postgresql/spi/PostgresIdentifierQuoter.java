package io.sqm.render.postgresql.spi;

import io.sqm.core.QuoteStyle;
import io.sqm.render.ansi.spi.AnsiIdentifierQuoter;
import io.sqm.render.spi.IdentifierQuoter;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * PostgreSQL identifier quoting rules.
 */
public class PostgresIdentifierQuoter implements IdentifierQuoter {
    // Unquoted identifiers must be simple and not reserved:
    private static final Pattern SIMPLE = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    // A pragmatic, compact reserved set (extend if you wish). Dialect owns the knowledge.
    private static final Set<String> RESERVED;

    static {
        RESERVED = new HashSet<>();
        RESERVED.addAll(AnsiIdentifierQuoter.RESERVED);
        RESERVED.addAll(Set.of(
            "LATERAL",
            "RETURNING",
            "GROUPS",
            "ILIKE",
            "ARRAY",
            "JSON",
            "JSONB",
            "TYPE",
            "CONFLICT",
            "DO",
            "NOTHING",
            "NOWAIT",
            "SKIP",
            "LOCKED",
            "SHARE"
        ));
    }

    /**
     * Creates PostgreSQL identifier quoter.
     */
    public PostgresIdentifierQuoter() {
    }

    /**
     * Puts the identifier inside the quotes supported by the dialect.
     *
     * @param identifier the identifier to quote.
     * @return a quoted identifier.
     */
    @Override
    public String quote(String identifier) {
        // Escape embedded double quotes per SQL standard by doubling them.
        String escaped = identifier.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    /**
     * Quotes identifier using the requested quote style when supported by PostgreSQL.
     *
     * @param identifier the identifier to quote.
     * @param quoteStyle the requested quote style.
     * @return a quoted identifier.
     */
    @Override
    public String quote(String identifier, QuoteStyle quoteStyle) {
        if (!supports(quoteStyle)) {
            throw new IllegalArgumentException("Unsupported quote style for PostgreSQL: " + quoteStyle);
        }
        return switch (quoteStyle == null ? QuoteStyle.NONE : quoteStyle) {
            case NONE -> quoteIfNeeded(identifier);
            case DOUBLE_QUOTE -> quote(identifier);
            default -> throw new IllegalArgumentException("Unsupported quote style for PostgreSQL: " + quoteStyle);
        };
    }

    /**
     * Puts the identifier inside the quotes supported by the dialect only if needed.
     * The possible implementation is to check whether the identifier is a keyword and only then to quote it.
     *
     * @param identifier the identifier to quote.
     * @return a quoted or an original identifier.
     */
    @Override
    public String quoteIfNeeded(String identifier) {
        if (needsQuoting(identifier)) return quote(identifier);
        return identifier;
    }

    /**
     * Quotes each part of the identifier separately.
     * <p>Example:</p>
     * <pre>
     *      {@code
     *      "s"."t" | [s].[t]
     *      }
     *  </pre>
     *
     * @param schemaOrNull the name of the schema if presented or NULL.
     * @param name         the name.
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
     * Indicates if the provided identifier needs to be quoted to avoid ambiguity.
     *
     * @param identifier the identifier.
     * @return True if the identifier must be quoted or False otherwise.
     */
    @Override
    public boolean needsQuoting(String identifier) {
        if (identifier == null || identifier.isEmpty()) return true;
        if (!SIMPLE.matcher(identifier).matches()) return true;
        // ANSI folds unquoted identifiers; keep rule simple: quote reserved words.
        return RESERVED.contains(identifier.toUpperCase());
    }

    /**
     * Indicates whether PostgreSQL supports the requested quote style.
     *
     * @param quoteStyle the quote style to check.
     * @return True for SQL standard double quotes and unquoted identifiers.
     */
    @Override
    public boolean supports(QuoteStyle quoteStyle) {
        return quoteStyle == null || quoteStyle == QuoteStyle.NONE || quoteStyle == QuoteStyle.DOUBLE_QUOTE;
    }
}
