package io.sqm.parser.core;

/**
 * Represents a token.
 *
 * @param type   a token type.
 * @param lexeme an actual text.
 * @param pos      a position in the tokens list.
 * @param quoteChar quote delimiter used for quoted identifiers, or {@code null} otherwise.
 */
public record Token(TokenType type, String lexeme, int pos, Character quoteChar) {
    /**
     * Creates a token without quote metadata.
     *
     * @param type   token type
     * @param lexeme token text
     * @param pos    source position
     */
    public Token(TokenType type, String lexeme, int pos) {
        this(type, lexeme, pos, null);
    }

    /**
     * Indicates whether this token is a quoted identifier.
     *
     * @return {@code true} if a quote delimiter is attached
     */
    public boolean quotedIdentifier() {
        return quoteChar != null;
    }

    @Override
    public String toString() {
        return type + (lexeme != null ? "[" + lexeme + "]" : "") + "@" + pos;
    }

    /**
     * Returns {@code true} if the given token starts exactly at the end of this token,
     * meaning the two tokens are lexically adjacent with no intervening whitespace
     * or other characters.
     *
     * <p>This is typically used to recognize composite lexemes such as:
     * <ul>
     *   <li>{@code $1} – positional parameters</li>
     *   <li>{@code :name} – named parameters</li>
     *   <li>{@code ::} – PostgreSQL cast operator</li>
     * </ul>
     *
     * @param next the token expected to immediately follow this token
     * @return {@code true} if {@code next} immediately follows this token
     */
    public boolean isImmediatelyFollowedBy(Token next) {
        return pos + 1 == next.pos;
    }

    /**
     * Returns {@code true} if the given token ends exactly at the start of this token,
     * meaning the two tokens are lexically adjacent with no intervening whitespace
     * or other characters.
     *
     * <p>This is the inverse of {@link #isImmediatelyFollowedBy(Token)} and is useful
     * when checking whether this token forms a composite lexeme with a preceding token.
     *
     * @param prev the token expected to immediately precede this token
     * @return {@code true} if {@code prev} immediately precedes this token
     */
    public boolean isImmediatelyPrecededBy(Token prev) {
        return prev.pos + 1 == pos;
    }

}
